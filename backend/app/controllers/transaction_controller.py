from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import HouseholdMember
from app.schemas.common import Page
from app.schemas.transaction import (
    TransactionCreate,
    TransactionFilterParams,
    TransactionResponse,
    TransactionUpdate,
)
from app.services.transaction_service import TransactionService


async def create_transaction(
    db: AsyncSession,
    household_id: int,
    acting_membership: HouseholdMember,
    payload: TransactionCreate,
) -> TransactionResponse:
    transaction = await TransactionService(db).create_transaction(
        household_id, acting_membership, payload
    )
    return TransactionResponse.model_validate(transaction)


async def list_transactions(
    db: AsyncSession, household_id: int, filters: TransactionFilterParams
) -> Page[TransactionResponse]:
    items, total = await TransactionService(db).list_transactions(household_id, filters)
    return Page[TransactionResponse](
        items=[TransactionResponse.model_validate(t) for t in items],
        total=total,
        limit=filters.limit,
        offset=filters.offset,
    )


async def get_transaction(
    db: AsyncSession, household_id: int, transaction_id: int
) -> TransactionResponse:
    transaction = await TransactionService(db).get_transaction_or_404(household_id, transaction_id)
    return TransactionResponse.model_validate(transaction)


async def update_transaction(
    db: AsyncSession,
    household_id: int,
    transaction_id: int,
    acting_membership: HouseholdMember,
    payload: TransactionUpdate,
) -> TransactionResponse:
    service = TransactionService(db)
    transaction = await service.get_transaction_or_404(household_id, transaction_id)
    updated = await service.update_transaction(transaction, acting_membership, payload)
    return TransactionResponse.model_validate(updated)


async def delete_transaction(
    db: AsyncSession, household_id: int, transaction_id: int, acting_membership: HouseholdMember
) -> None:
    service = TransactionService(db)
    transaction = await service.get_transaction_or_404(household_id, transaction_id)
    await service.delete_transaction(transaction, acting_membership)
