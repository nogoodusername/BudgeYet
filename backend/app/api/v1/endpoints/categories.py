from typing import List, Optional

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_household, get_db, require_admin_membership
from app.controllers import category_controller
from app.models.household import Household, HouseholdMember
from app.schemas.category import (
    CategoryCreate,
    CategoryResponse,
    CategoryUpdate,
    CategoryWithStats,
)

router = APIRouter(prefix="/households/{household_id}/categories", tags=["Categories"])


@router.post("", response_model=CategoryResponse, status_code=status.HTTP_201_CREATED)
async def create_category(
    household_id: int,
    payload: CategoryCreate,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Add a category with a monthly limit. Admin only."""
    return await category_controller.create_category(db, household_id, payload)


@router.get("", response_model=List[CategoryWithStats])
async def list_categories(
    household: Household = Depends(get_current_household),
    db: AsyncSession = Depends(get_db),
):
    """Categories with current-cycle spend stats, sorted by % utilized descending."""
    return await category_controller.list_categories(db, household)


@router.get("/{category_id}", response_model=CategoryWithStats)
async def get_category(
    category_id: int,
    household: Household = Depends(get_current_household),
    db: AsyncSession = Depends(get_db),
):
    return await category_controller.get_category(db, household, category_id)


@router.patch("/{category_id}", response_model=CategoryResponse)
async def update_category(
    household_id: int,
    category_id: int,
    payload: CategoryUpdate,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Edit name/icon/limit. Admin only."""
    return await category_controller.update_category(db, household_id, category_id, payload)


@router.delete("/{category_id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_category(
    household_id: int,
    category_id: int,
    reassign_to_category_id: Optional[int] = None,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Delete a category. Admin only. If it has transactions, pass
    reassign_to_category_id to move them first — otherwise this returns 409."""
    await category_controller.delete_category(db, household_id, category_id, reassign_to_category_id)
