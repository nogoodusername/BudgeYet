from datetime import date
from typing import Optional

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_db, get_household_membership
from app.controllers import transaction_controller
from app.models.household import HouseholdMember
from app.models.transaction import PaymentMode, TransactionType
from app.schemas.common import Page
from app.schemas.transaction import (
    TransactionCreate,
    TransactionFilterParams,
    TransactionResponse,
    TransactionUpdate,
)

router = APIRouter(prefix="/households/{household_id}/transactions", tags=["Transactions"])


@router.post("", response_model=TransactionResponse, status_code=status.HTTP_201_CREATED)
async def create_transaction(
    household_id: int,
    payload: TransactionCreate,
    membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Log a transaction. `paid_by_id` defaults to the caller. Future dates are rejected."""
    return await transaction_controller.create_transaction(db, household_id, membership, payload)


@router.get("", response_model=Page[TransactionResponse])
async def list_transactions(
    household_id: int,
    category_id: Optional[int] = None,
    paid_by_id: Optional[int] = None,
    type: Optional[TransactionType] = None,
    payment_mode: Optional[PaymentMode] = None,
    date_from: Optional[date] = None,
    date_to: Optional[date] = None,
    search: Optional[str] = None,
    limit: int = 50,
    offset: int = 0,
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Full transaction history, grouped by date (most recent first), with filters."""
    filters = TransactionFilterParams(
        category_id=category_id,
        paid_by_id=paid_by_id,
        type=type,
        payment_mode=payment_mode,
        date_from=date_from,
        date_to=date_to,
        search=search,
        limit=limit,
        offset=offset,
    )
    return await transaction_controller.list_transactions(db, household_id, filters)


@router.get("/{transaction_id}", response_model=TransactionResponse)
async def get_transaction(
    household_id: int,
    transaction_id: int,
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    return await transaction_controller.get_transaction(db, household_id, transaction_id)


@router.patch("/{transaction_id}", response_model=TransactionResponse)
async def update_transaction(
    household_id: int,
    transaction_id: int,
    payload: TransactionUpdate,
    membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Members may edit only their own transactions; Admins may edit any."""
    return await transaction_controller.update_transaction(
        db, household_id, transaction_id, membership, payload
    )


@router.delete("/{transaction_id}", status_code=status.HTTP_204_NO_CONTENT, response_model=None)
async def delete_transaction(
    household_id: int,
    transaction_id: int,
    membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Members may delete only their own transactions; Admins may delete any."""
    await transaction_controller.delete_transaction(db, household_id, transaction_id, membership)
