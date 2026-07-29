from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.repositories.user_repository import UserRepository
from app.schemas.user import UserUpdate


class UserService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.users = UserRepository(db)

    async def update_profile(self, user: User, payload: UserUpdate) -> User:
        return await self.users.update(
            user,
            full_name=payload.full_name,
            nickname=payload.nickname,
            avatar_url=payload.avatar_url,
            display_mode=payload.display_mode,
        )
