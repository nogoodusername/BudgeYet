from typing import List, Optional

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import (
    get_current_household,
    get_db,
    get_household_membership,
    require_admin_membership,
)
from app.controllers import budget_controller
from app.models.household import Household, HouseholdMember
from app.schemas.budget import BudgetCreate, BudgetResponse, BudgetUpdate, BudgetWithStats

router = APIRouter(prefix="/households/{household_id}/budgets", tags=["Budgets"])


@router.post("", response_model=BudgetResponse, status_code=status.HTTP_201_CREATED)
async def create_budget(
    household_id: int,
    payload: BudgetCreate,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Create a budget for a cycle (defaults to the current one). Admin only."""
    return await budget_controller.create_budget(db, household_id, payload)


@router.post("/rollover", response_model=Optional[BudgetResponse])
async def rollover_budget(
    household_id: int,
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Carry the household's latest budget into the current cycle if it has none
    yet (goal amount + regenerated name only). Idempotent — returns the current
    cycle's budget, or null if the household has never had one.

    Any member may trigger this: there's no request body and no choice to make,
    it only copies the household's own most recent budget forward, and gating it
    on admin would strand members on the "Set Up Budget" screen until an admin
    next opens the app.
    """
    return await budget_controller.rollover_budget(db, household_id)


@router.get("", response_model=List[BudgetResponse])
async def list_budgets(
    household_id: int,
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Budget history for the household — visible to any member."""
    return await budget_controller.list_budgets(db, household_id)


@router.get("/current", response_model=Optional[BudgetWithStats])
async def get_current_budget(
    household: Household = Depends(get_current_household),
    db: AsyncSession = Depends(get_db),
):
    """Current cycle's budget with spend stats, or null if none has been set up."""
    return await budget_controller.get_current_budget(db, household)


@router.patch("/{budget_id}", response_model=BudgetResponse)
async def update_budget(
    household_id: int,
    budget_id: int,
    payload: BudgetUpdate,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Update a budget's name/goal amount. Admin only."""
    return await budget_controller.update_budget(db, household_id, budget_id, payload)
