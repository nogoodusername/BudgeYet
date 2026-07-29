from datetime import datetime
from typing import Sequence, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError, PermissionDeniedError, ValidationAppError
from app.models.household import HouseholdMember, MemberRole
from app.models.transaction import Transaction
from app.repositories.category_repository import CategoryRepository
from app.repositories.household_member_repository import HouseholdMemberRepository
from app.repositories.transaction_repository import TransactionRepository
from app.schemas.transaction import TransactionCreate, TransactionFilterParams, TransactionUpdate


class TransactionService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.transactions = TransactionRepository(db)
        self.categories = CategoryRepository(db)
        self.members = HouseholdMemberRepository(db)

    async def get_transaction_or_404(self, household_id: int, transaction_id: int) -> Transaction:
        transaction = await self.transactions.get_by_id(transaction_id)
        if transaction is None or transaction.household_id != household_id:
            raise NotFoundError("Transaction not found")
        return transaction

    def _assert_future_date_allowed(self, transaction_date: datetime) -> None:
        if transaction_date.date() > datetime.utcnow().date():
            raise ValidationAppError("Future-dated transactions are not allowed")

    async def _assert_paid_by_is_member(self, household_id: int, paid_by_id: int) -> None:
        member = await self.members.get_by_household_and_user(household_id, paid_by_id)
        if member is None:
            raise ValidationAppError("paid_by_id must be a member of this household")

    async def _assert_category_in_household(self, household_id: int, category_id: int) -> None:
        category = await self.categories.get_by_id(category_id)
        if category is None or category.household_id != household_id:
            raise ValidationAppError("category_id does not belong to this household")

    def _assert_can_modify(self, transaction: Transaction, acting_membership: HouseholdMember) -> None:
        is_owner = transaction.created_by_id == acting_membership.user_id
        if acting_membership.role != MemberRole.ADMIN and not is_owner:
            raise PermissionDeniedError("You can only edit or delete your own transactions")

    async def create_transaction(
        self, household_id: int, acting_membership: HouseholdMember, payload: TransactionCreate
    ) -> Transaction:
        transaction_date = payload.transaction_date or datetime.utcnow()
        self._assert_future_date_allowed(transaction_date)

        paid_by_id = payload.paid_by_id or acting_membership.user_id
        await self._assert_paid_by_is_member(household_id, paid_by_id)

        if payload.category_id is not None:
            await self._assert_category_in_household(household_id, payload.category_id)

        return await self.transactions.create(
            household_id=household_id,
            category_id=payload.category_id,
            paid_by_id=paid_by_id,
            created_by_id=acting_membership.user_id,
            type=payload.type,
            amount=payload.amount,
            merchant=payload.merchant,
            payment_mode=payload.payment_mode,
            notes=payload.notes,
            transaction_date=transaction_date,
        )

    async def list_transactions(
        self, household_id: int, filters: TransactionFilterParams
    ) -> Tuple[Sequence[Transaction], int]:
        if (
            filters.amount_min is not None
            and filters.amount_max is not None
            and filters.amount_min > filters.amount_max
        ):
            raise ValidationAppError("amount_min must not be greater than amount_max")

        return await self.transactions.list(
            household_id,
            category_id=filters.category_id,
            paid_by_id=filters.paid_by_id,
            type=filters.type,
            payment_mode=filters.payment_mode,
            date_from=filters.date_from,
            date_to=filters.date_to,
            search=filters.search,
            amount_min=filters.amount_min,
            amount_max=filters.amount_max,
            limit=filters.limit,
            offset=filters.offset,
        )

    async def update_transaction(
        self,
        transaction: Transaction,
        acting_membership: HouseholdMember,
        payload: TransactionUpdate,
    ) -> Transaction:
        self._assert_can_modify(transaction, acting_membership)

        if payload.transaction_date is not None:
            self._assert_future_date_allowed(payload.transaction_date)
        if payload.paid_by_id is not None:
            await self._assert_paid_by_is_member(transaction.household_id, payload.paid_by_id)
        if payload.category_id is not None:
            await self._assert_category_in_household(transaction.household_id, payload.category_id)

        return await self.transactions.update(
            transaction,
            amount=payload.amount,
            merchant=payload.merchant,
            type=payload.type,
            category_id=payload.category_id,
            paid_by_id=payload.paid_by_id,
            payment_mode=payload.payment_mode,
            notes=payload.notes,
            transaction_date=payload.transaction_date,
        )

    async def delete_transaction(
        self, transaction: Transaction, acting_membership: HouseholdMember
    ) -> None:
        self._assert_can_modify(transaction, acting_membership)
        await self.transactions.delete(transaction)
