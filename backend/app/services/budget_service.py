from typing import Optional, Sequence

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.models.budget import Budget
from app.models.household import Household
from app.models.transaction import TransactionType
from app.repositories.budget_repository import BudgetRepository
from app.repositories.transaction_repository import TransactionRepository
from app.schemas.budget import BudgetCreate, BudgetResponse, BudgetUpdate, BudgetWithStats
from app.services.cycle_utils import budget_status, get_current_cycle_bounds


class BudgetService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.budgets = BudgetRepository(db)
        self.transactions = TransactionRepository(db)

    async def get_budget_or_404(self, household_id: int, budget_id: int) -> Budget:
        budget = await self.budgets.get_by_id(budget_id)
        if budget is None or budget.household_id != household_id:
            raise NotFoundError("Budget not found")
        return budget

    async def list_budgets(self, household_id: int) -> Sequence[Budget]:
        return await self.budgets.list_by_household(household_id)

    async def create_budget(self, household: Household, payload: BudgetCreate) -> Budget:
        bounds = get_current_cycle_bounds(household.cycle_start_day)
        month = payload.month or bounds.label_month
        year = payload.year or bounds.label_year

        existing = await self.budgets.get_for_cycle(household.id, month, year)
        if existing is not None:
            raise ConflictError(
                f"A budget already exists for {month}/{year} — update it instead of creating a new one"
            )

        return await self.budgets.create(
            household_id=household.id,
            name=payload.name,
            monthly_goal_amount=payload.monthly_goal_amount,
            month=month,
            year=year,
        )

    async def update_budget(self, budget: Budget, payload: BudgetUpdate) -> Budget:
        return await self.budgets.update(
            budget, name=payload.name, monthly_goal_amount=payload.monthly_goal_amount
        )

    async def get_current_budget_with_stats(
        self, household: Household
    ) -> Optional[BudgetWithStats]:
        bounds = get_current_cycle_bounds(household.cycle_start_day)
        budget = await self.budgets.get_for_cycle(household.id, bounds.label_month, bounds.label_year)
        if budget is None:
            return None

        spent = await self.transactions.sum_spent(
            household.id,
            date_from=bounds.start,
            date_to=bounds.end,
            type=TransactionType.EXPENSE,
        )
        percent_used = (spent / budget.monthly_goal_amount) * 100
        base = BudgetResponse.model_validate(budget).model_dump()
        return BudgetWithStats(
            **base,
            spent=spent,
            remaining=budget.monthly_goal_amount - spent,
            percent_used=round(percent_used, 2),
            status=budget_status(percent_used),
        )
