from typing import List, Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import Household
from app.schemas.category import (
    CategoryCreate,
    CategoryResponse,
    CategoryUpdate,
    CategoryWithStats,
)
from app.services.category_service import CategoryService


async def create_category(
    db: AsyncSession, household_id: int, payload: CategoryCreate
) -> CategoryResponse:
    category = await CategoryService(db).create_category(household_id, payload)
    return CategoryResponse.model_validate(category)


async def list_categories(db: AsyncSession, household: Household) -> List[CategoryWithStats]:
    return await CategoryService(db).list_categories_with_stats(household)


async def get_category(
    db: AsyncSession, household: Household, category_id: int
) -> CategoryWithStats:
    service = CategoryService(db)
    category = await service.get_category_or_404(household.id, category_id)
    return await service.get_category_with_stats(category, household)


async def update_category(
    db: AsyncSession, household_id: int, category_id: int, payload: CategoryUpdate
) -> CategoryResponse:
    service = CategoryService(db)
    category = await service.get_category_or_404(household_id, category_id)
    updated = await service.update_category(category, payload)
    return CategoryResponse.model_validate(updated)


async def delete_category(
    db: AsyncSession,
    household_id: int,
    category_id: int,
    reassign_to_category_id: Optional[int],
) -> None:
    service = CategoryService(db)
    category = await service.get_category_or_404(household_id, category_id)
    await service.delete_category(category, reassign_to_category_id)
