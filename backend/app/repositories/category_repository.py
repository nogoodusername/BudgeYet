from typing import Optional, Sequence
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.category import Category


class CategoryRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, category_id: int) -> Optional[Category]:
        return await self.db.get(Category, category_id)

    async def list_by_household(self, household_id: int) -> Sequence[Category]:
        result = await self.db.execute(
            select(Category).where(Category.household_id == household_id).order_by(Category.name)
        )
        return result.scalars().all()

    async def create(
        self, *, household_id: int, name: str, icon: str, monthly_limit: float
    ) -> Category:
        category = Category(
            household_id=household_id, name=name, icon=icon, monthly_limit=monthly_limit
        )
        self.db.add(category)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def update(self, category: Category, **fields) -> Category:
        for key, value in fields.items():
            if value is not None:
                setattr(category, key, value)
        await self.db.flush()
        await self.db.refresh(category)
        return category

    async def delete(self, category: Category) -> None:
        await self.db.delete(category)
        await self.db.flush()
