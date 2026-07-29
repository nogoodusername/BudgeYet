from typing import Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.models.household import Household, HouseholdMember


class HouseholdRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, household_id: int) -> Optional[Household]:
        result = await self.db.execute(
            select(Household)
            .options(selectinload(Household.members).selectinload(HouseholdMember.user))
            .where(Household.id == household_id)
        )
        return result.scalar_one_or_none()

    async def create(self, *, name: str, currency: str, language: str, cycle_start_day: int) -> Household:
        household = Household(
            name=name, currency=currency, language=language, cycle_start_day=cycle_start_day
        )
        self.db.add(household)
        await self.db.flush()
        await self.db.refresh(household)
        return household

    async def update(self, household: Household, **fields) -> Household:
        for key, value in fields.items():
            if value is not None:
                setattr(household, key, value)
        await self.db.flush()
        await self.db.refresh(household)
        return household
