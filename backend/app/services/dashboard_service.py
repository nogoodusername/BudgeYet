from typing import Sequence, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household
from app.models.transaction import Transaction
from app.repositories.transaction_repository import TransactionRepository
from app.schemas.dashboard import DashboardResponse
from app.services.budget_service import BudgetService
from app.services.category_service import CategoryService


class DashboardService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.budget_service = BudgetService(db)
        self.category_service = CategoryService(db)
        self.transactions = TransactionRepository(db)

    async def get_dashboard(self, household: Household) -> DashboardResponse:
        budget = await self.budget_service.get_current_budget_with_stats(household)
        categories = await self.category_service.list_categories_with_stats(household)
        _, total_transactions = await self.transactions.list_recent(household.id, limit=1, offset=0)

        return DashboardResponse(
            has_budget=budget is not None,
            has_transactions=total_transactions > 0,
            budget=budget,
            categories=categories,
        )

    async def get_activity_feed(
        self, household_id: int, *, limit: int, offset: int
    ) -> Tuple[Sequence[Transaction], int]:
        return await self.transactions.list_recent(household_id, limit=limit, offset=offset)
