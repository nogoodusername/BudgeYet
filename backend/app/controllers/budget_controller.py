from typing import Optional, Sequence

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household
from app.schemas.budget import BudgetCreate, BudgetResponse, BudgetUpdate, BudgetWithStats
from app.services.budget_service import BudgetService
from app.services.household_service import HouseholdService


async def create_budget(
    db: AsyncSession, household_id: int, payload: BudgetCreate
) -> BudgetResponse:
    household = await HouseholdService(db).get_household_or_404(household_id)
    budget = await BudgetService(db).create_budget(household, payload)
    return BudgetResponse.model_validate(budget)


async def list_budgets(db: AsyncSession, household_id: int) -> Sequence[BudgetResponse]:
    budgets = await BudgetService(db).list_budgets(household_id)
    return [BudgetResponse.model_validate(b) for b in budgets]


async def get_current_budget(
    db: AsyncSession, household: Household
) -> Optional[BudgetWithStats]:
    return await BudgetService(db).get_current_budget_with_stats(household)


async def update_budget(
    db: AsyncSession, household_id: int, budget_id: int, payload: BudgetUpdate
) -> BudgetResponse:
    service = BudgetService(db)
    budget = await service.get_budget_or_404(household_id, budget_id)
    updated = await service.update_budget(budget, payload)
    return BudgetResponse.model_validate(updated)
