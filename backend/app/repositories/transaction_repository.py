from datetime import date, datetime
from decimal import Decimal
from typing import Dict, Optional, Sequence, Tuple
from sqlalchemy import func, or_, select, update as sql_update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.models.category import Category
from app.models.transaction import PaymentMode, Transaction, TransactionType


class TransactionRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    def _with_relations(self, stmt):
        return stmt.options(
            selectinload(Transaction.category),
            selectinload(Transaction.paid_by_user),
            selectinload(Transaction.created_by_user),
        )

    async def get_by_id(self, transaction_id: int) -> Optional[Transaction]:
        result = await self.db.execute(
            self._with_relations(select(Transaction)).where(Transaction.id == transaction_id)
        )
        return result.scalar_one_or_none()

    async def list(
        self,
        household_id: int,
        *,
        category_id: Optional[int] = None,
        paid_by_id: Optional[int] = None,
        type: Optional[TransactionType] = None,
        payment_mode: Optional[PaymentMode] = None,
        date_from: Optional[date] = None,
        date_to: Optional[date] = None,
        search: Optional[str] = None,
        amount_min: Optional[float] = None,
        amount_max: Optional[float] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> Tuple[Sequence[Transaction], int]:
        conditions = [Transaction.household_id == household_id]
        if category_id is not None:
            conditions.append(Transaction.category_id == category_id)
        if paid_by_id is not None:
            conditions.append(Transaction.paid_by_id == paid_by_id)
        if type is not None:
            conditions.append(Transaction.type == type)
        if payment_mode is not None:
            conditions.append(Transaction.payment_mode == payment_mode)
        if date_from is not None:
            conditions.append(Transaction.transaction_date >= date_from)
        if date_to is not None:
            conditions.append(Transaction.transaction_date < date_to)
        if amount_min is not None:
            conditions.append(Transaction.amount >= amount_min)
        if amount_max is not None:
            conditions.append(Transaction.amount <= amount_max)
        if search:
            pattern = f"%{search}%"
            conditions.append(or_(Transaction.merchant.ilike(pattern), Category.name.ilike(pattern)))

        def _base(stmt):
            # Only needed to match search text against the category name.
            return stmt.outerjoin(Category, Transaction.category_id == Category.id) if search else stmt

        count_result = await self.db.execute(
            _base(select(func.count()).select_from(Transaction)).where(*conditions)
        )
        total = count_result.scalar_one()

        result = await self.db.execute(
            _base(self._with_relations(select(Transaction)))
            .where(*conditions)
            .order_by(Transaction.transaction_date.desc(), Transaction.id.desc())
            .limit(limit)
            .offset(offset)
        )
        return result.scalars().all(), total

    async def list_recent(
        self, household_id: int, *, limit: int = 20, offset: int = 0
    ) -> Tuple[Sequence[Transaction], int]:
        count_result = await self.db.execute(
            select(func.count())
            .select_from(Transaction)
            .where(Transaction.household_id == household_id)
        )
        total = count_result.scalar_one()

        result = await self.db.execute(
            self._with_relations(select(Transaction))
            .where(Transaction.household_id == household_id)
            .order_by(Transaction.created_at.desc(), Transaction.id.desc())
            .limit(limit)
            .offset(offset)
        )
        return result.scalars().all(), total

    async def create(
        self,
        *,
        household_id: int,
        category_id: Optional[int],
        paid_by_id: int,
        created_by_id: int,
        type: TransactionType,
        amount: Decimal,
        merchant: str,
        payment_mode: PaymentMode,
        notes: Optional[str],
        transaction_date: datetime,
    ) -> Transaction:
        transaction = Transaction(
            household_id=household_id,
            category_id=category_id,
            paid_by_id=paid_by_id,
            created_by_id=created_by_id,
            type=type,
            amount=amount,
            merchant=merchant,
            payment_mode=payment_mode,
            notes=notes,
            transaction_date=transaction_date,
        )
        self.db.add(transaction)
        await self.db.flush()
        return await self.get_by_id(transaction.id)

    async def update(self, transaction: Transaction, **fields) -> Transaction:
        for key, value in fields.items():
            if value is not None:
                setattr(transaction, key, value)
        await self.db.flush()
        await self.db.refresh(transaction)
        return transaction

    async def delete(self, transaction: Transaction) -> None:
        await self.db.delete(transaction)
        await self.db.flush()

    async def count_by_category(self, category_id: int) -> int:
        result = await self.db.execute(
            select(func.count()).where(Transaction.category_id == category_id)
        )
        return result.scalar_one()

    async def reassign_category(self, from_category_id: int, to_category_id: int) -> None:
        await self.db.execute(
            sql_update(Transaction)
            .where(Transaction.category_id == from_category_id)
            .values(category_id=to_category_id)
        )
        await self.db.flush()

    async def sum_spent(
        self,
        household_id: int,
        *,
        date_from: datetime,
        date_to: datetime,
        type: TransactionType = TransactionType.EXPENSE,
    ) -> Decimal:
        result = await self.db.execute(
            select(func.coalesce(func.sum(Transaction.amount), 0)).where(
                Transaction.household_id == household_id,
                Transaction.type == type,
                Transaction.transaction_date >= date_from,
                Transaction.transaction_date < date_to,
            )
        )
        return Decimal(result.scalar_one())

    async def sum_spent_by_category(
        self,
        household_id: int,
        *,
        date_from: datetime,
        date_to: datetime,
        type: TransactionType = TransactionType.EXPENSE,
    ) -> Dict[int, Decimal]:
        result = await self.db.execute(
            select(Transaction.category_id, func.coalesce(func.sum(Transaction.amount), 0))
            .where(
                Transaction.household_id == household_id,
                Transaction.type == type,
                Transaction.transaction_date >= date_from,
                Transaction.transaction_date < date_to,
                Transaction.category_id.is_not(None),
            )
            .group_by(Transaction.category_id)
        )
        return {category_id: Decimal(total) for category_id, total in result.all()}
