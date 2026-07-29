from typing import Optional, Sequence
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from app.models.household import HouseholdMember, MemberRole


class HouseholdMemberRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, member_id: int) -> Optional[HouseholdMember]:
        result = await self.db.execute(
            select(HouseholdMember)
            .options(selectinload(HouseholdMember.user))
            .where(HouseholdMember.id == member_id)
        )
        return result.scalar_one_or_none()

    async def get_by_household_and_user(
        self, household_id: int, user_id: int
    ) -> Optional[HouseholdMember]:
        result = await self.db.execute(
            select(HouseholdMember)
            .options(selectinload(HouseholdMember.user))
            .where(
                HouseholdMember.household_id == household_id,
                HouseholdMember.user_id == user_id,
            )
        )
        return result.scalar_one_or_none()

    async def get_by_user(self, user_id: int) -> Optional[HouseholdMember]:
        """v1 caps users to a single household — return that membership, if any."""
        result = await self.db.execute(
            select(HouseholdMember).where(HouseholdMember.user_id == user_id)
        )
        return result.scalar_one_or_none()

    async def list_by_household(self, household_id: int) -> Sequence[HouseholdMember]:
        result = await self.db.execute(
            select(HouseholdMember)
            .options(selectinload(HouseholdMember.user))
            .where(HouseholdMember.household_id == household_id)
        )
        return result.scalars().all()

    async def count_by_household(self, household_id: int) -> int:
        result = await self.db.execute(
            select(func.count()).where(HouseholdMember.household_id == household_id)
        )
        return result.scalar_one()

    async def count_admins(self, household_id: int) -> int:
        result = await self.db.execute(
            select(func.count()).where(
                HouseholdMember.household_id == household_id,
                HouseholdMember.role == MemberRole.ADMIN,
            )
        )
        return result.scalar_one()

    async def create(self, *, household_id: int, user_id: int, role: MemberRole) -> HouseholdMember:
        member = HouseholdMember(household_id=household_id, user_id=user_id, role=role)
        self.db.add(member)
        await self.db.flush()
        await self.db.refresh(member)
        return member

    async def update_role(self, member: HouseholdMember, role: MemberRole) -> HouseholdMember:
        member.role = role
        await self.db.flush()
        await self.db.refresh(member)
        return member

    async def delete(self, member: HouseholdMember) -> None:
        await self.db.delete(member)
        await self.db.flush()
