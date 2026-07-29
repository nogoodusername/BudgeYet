from typing import List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError, ValidationAppError
from app.models.category import Category
from app.models.household import Household
from app.models.transaction import TransactionType
from app.repositories.category_repository import CategoryRepository
from app.repositories.transaction_repository import TransactionRepository
from app.schemas.category import CategoryCreate, CategoryResponse, CategoryUpdate, CategoryWithStats
from app.services.cycle_utils import budget_status, get_current_cycle_bounds


class CategoryService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.categories = CategoryRepository(db)
        self.transactions = TransactionRepository(db)

    async def get_category_or_404(self, household_id: int, category_id: int) -> Category:
        category = await self.categories.get_by_id(category_id)
        if category is None or category.household_id != household_id:
            raise NotFoundError("Category not found")
        return category

    async def create_category(self, household_id: int, payload: CategoryCreate) -> Category:
        return await self.categories.create(
            household_id=household_id,
            name=payload.name,
            icon=payload.icon,
            monthly_limit=payload.monthly_limit,
        )

    async def update_category(self, category: Category, payload: CategoryUpdate) -> Category:
        return await self.categories.update(
            category, name=payload.name, icon=payload.icon, monthly_limit=payload.monthly_limit
        )

    async def delete_category(
        self, category: Category, reassign_to_category_id: Optional[int]
    ) -> None:
        usage_count = await self.transactions.count_by_category(category.id)
        if usage_count > 0:
            if reassign_to_category_id is None:
                raise ConflictError(
                    "This category has existing transactions — reassign them to another "
                    "category before deleting (pass reassign_to_category_id)"
                )
            if reassign_to_category_id == category.id:
                raise ValidationAppError("Cannot reassign a category's transactions to itself")

            target = await self.categories.get_by_id(reassign_to_category_id)
            if target is None or target.household_id != category.household_id:
                raise NotFoundError("Target category for reassignment not found")

            await self.transactions.reassign_category(category.id, reassign_to_category_id)

        await self.categories.delete(category)

    async def _with_stats(
        self, category: Category, spent_by_category: dict[int, float]
    ) -> CategoryWithStats:
        spent = spent_by_category.get(category.id, 0.0)
        percent_used = (spent / category.monthly_limit * 100) if category.monthly_limit > 0 else 0.0
        base = CategoryResponse.model_validate(category).model_dump()
        return CategoryWithStats(
            **base,
            spent=spent,
            remaining=category.monthly_limit - spent,
            percent_used=round(percent_used, 2),
            status=budget_status(percent_used),
        )

    async def list_categories_with_stats(self, household: Household) -> List[CategoryWithStats]:
        categories = await self.categories.list_by_household(household.id)
        bounds = get_current_cycle_bounds(household.cycle_start_day)
        spent_by_category = await self.transactions.sum_spent_by_category(
            household.id, date_from=bounds.start, date_to=bounds.end, type=TransactionType.EXPENSE
        )
        stats = [await self._with_stats(c, spent_by_category) for c in categories]
        return sorted(stats, key=lambda c: c.percent_used, reverse=True)

    async def get_category_with_stats(
        self, category: Category, household: Household
    ) -> CategoryWithStats:
        bounds = get_current_cycle_bounds(household.cycle_start_day)
        spent_by_category = await self.transactions.sum_spent_by_category(
            household.id, date_from=bounds.start, date_to=bounds.end, type=TransactionType.EXPENSE
        )
        return await self._with_stats(category, spent_by_category)
