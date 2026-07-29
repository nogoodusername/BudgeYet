from decimal import Decimal
from typing import Optional, Sequence
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.budget import Budget


class BudgetRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, budget_id: int) -> Optional[Budget]:
        return await self.db.get(Budget, budget_id)

    async def get_for_cycle(self, household_id: int, month: int, year: int) -> Optional[Budget]:
        result = await self.db.execute(
            select(Budget).where(
                Budget.household_id == household_id,
                Budget.month == month,
                Budget.year == year,
            )
        )
        return result.scalar_one_or_none()

    async def list_by_household(self, household_id: int) -> Sequence[Budget]:
        result = await self.db.execute(
            select(Budget)
            .where(Budget.household_id == household_id)
            .order_by(Budget.year.desc(), Budget.month.desc())
        )
        return result.scalars().all()

    async def create(
        self, *, household_id: int, name: str, monthly_goal_amount: Decimal, month: int, year: int
    ) -> Budget:
        budget = Budget(
            household_id=household_id,
            name=name,
            monthly_goal_amount=monthly_goal_amount,
            month=month,
            year=year,
        )
        self.db.add(budget)
        await self.db.flush()
        await self.db.refresh(budget)
        return budget

    async def update(self, budget: Budget, **fields) -> Budget:
        for key, value in fields.items():
            if value is not None:
                setattr(budget, key, value)
        await self.db.flush()
        await self.db.refresh(budget)
        return budget
