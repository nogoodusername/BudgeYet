from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.auth import LoginRequest, LoginResponse
from app.schemas.user import UserCreate, UserResponse
from app.services.auth_service import AuthService


async def signup(db: AsyncSession, payload: UserCreate) -> UserResponse:
    user = await AuthService(db).signup(payload)
    return UserResponse.model_validate(user)


async def login(db: AsyncSession, payload: LoginRequest) -> LoginResponse:
    user, token = await AuthService(db).login(payload.email, payload.pin)
    return LoginResponse(
        access_token=token, user=UserResponse.model_validate(user)
    )
