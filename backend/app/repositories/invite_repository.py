from datetime import datetime
from typing import Optional, Sequence
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.invite import Invite


class InviteRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, invite_id: int) -> Optional[Invite]:
        return await self.db.get(Invite, invite_id)

    async def get_by_token(self, token: str) -> Optional[Invite]:
        # Case-insensitive so a join code retyped in the wrong case still matches
        # (generate_invite_token issues uppercase codes; this covers user input).
        result = await self.db.execute(
            select(Invite).where(func.upper(Invite.token) == token.upper())
        )
        return result.scalar_one_or_none()

    async def list_pending_by_household(self, household_id: int) -> Sequence[Invite]:
        result = await self.db.execute(
            select(Invite).where(
                Invite.household_id == household_id,
                Invite.revoked.is_(False),
                Invite.accepted_at.is_(None),
            )
        )
        return result.scalars().all()

    async def create(
        self,
        *,
        household_id: int,
        invited_by_id: int,
        email: Optional[str],
        token: str,
        expires_at: datetime,
    ) -> Invite:
        invite = Invite(
            household_id=household_id,
            invited_by_id=invited_by_id,
            email=email,
            token=token,
            expires_at=expires_at,
        )
        self.db.add(invite)
        await self.db.flush()
        await self.db.refresh(invite)
        return invite

    async def mark_accepted(self, invite: Invite) -> Invite:
        invite.accepted_at = datetime.utcnow()
        await self.db.flush()
        await self.db.refresh(invite)
        return invite

    async def revoke(self, invite: Invite) -> Invite:
        invite.revoked = True
        await self.db.flush()
        await self.db.refresh(invite)
        return invite
