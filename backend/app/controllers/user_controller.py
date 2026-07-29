from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.schemas.user import UserResponse, UserUpdate
from app.services.user_service import UserService


async def get_me(current_user: User) -> UserResponse:
    return UserResponse.model_validate(current_user)


async def update_me(db: AsyncSession, current_user: User, payload: UserUpdate) -> UserResponse:
    user = await UserService(db).update_profile(current_user, payload)
    return UserResponse.model_validate(user)
