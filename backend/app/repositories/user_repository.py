from datetime import datetime
from typing import Optional
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.user import User


class UserRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def get_by_id(self, user_id: int) -> Optional[User]:
        return await self.db.get(User, user_id)

    async def get_by_email(self, email: str) -> Optional[User]:
        result = await self.db.execute(select(User).where(User.email == email))
        return result.scalar_one_or_none()

    async def create(self, *, email: str, full_name: str, nickname: str, pin_hash: str) -> User:
        user = User(email=email, full_name=full_name, nickname=nickname, pin_hash=pin_hash)
        self.db.add(user)
        await self.db.flush()
        await self.db.refresh(user)
        return user

    async def update(self, user: User, **fields) -> User:
        for key, value in fields.items():
            if value is not None:
                setattr(user, key, value)
        await self.db.flush()
        await self.db.refresh(user)
        return user

    async def record_failed_login(self, user: User, *, locked_until: Optional[datetime]) -> User:
        user.failed_login_attempts += 1
        user.locked_until = locked_until
        await self.db.flush()
        await self.db.refresh(user)
        return user

    async def reset_login_attempts(self, user: User) -> User:
        user.failed_login_attempts = 0
        user.locked_until = None
        await self.db.flush()
        await self.db.refresh(user)
        return user
