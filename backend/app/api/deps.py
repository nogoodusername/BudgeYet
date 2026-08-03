from typing import AsyncGenerator

from fastapi import Depends, Header
from jose import JWTError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_async_db
from app.core.exceptions import AuthenticationError, NotFoundError, PermissionDeniedError
from app.core.security import decode_access_token
from app.models.household import Household, HouseholdMember, MemberRole
from app.models.user import User
from app.repositories.household_member_repository import HouseholdMemberRepository
from app.repositories.user_repository import UserRepository
from app.services.household_service import HouseholdService


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency wrapper for database sessions."""
    async for session in get_async_db():
        yield session


async def get_current_user(
    authorization: str | None = Header(default=None),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Resolve the current user from a `Bearer <token>` Authorization header."""
    if not authorization or not authorization.lower().startswith("bearer "):
        raise AuthenticationError("Missing or malformed Authorization header")

    token = authorization.split(" ", 1)[1].strip()
    try:
        user_id = decode_access_token(token)
    except JWTError as exc:
        raise AuthenticationError("Invalid or expired access token") from exc

    user = await UserRepository(db).get_by_id(user_id)
    if user is None:
        raise AuthenticationError("User for this token no longer exists")
    return user


async def get_household_membership(
    household_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> HouseholdMember:
    """Resolve the current user's membership in the household referenced by the path.

    Raises NotFoundError if the user is not a member — membership (not existence)
    is what gates visibility, so a non-member sees the same 404 as a bad household id.
    """
    membership = await HouseholdMemberRepository(db).get_by_household_and_user(
        household_id, current_user.id
    )
    if membership is None:
        raise NotFoundError("Household not found")
    return membership


async def require_admin_membership(
    membership: HouseholdMember = Depends(get_household_membership),
) -> HouseholdMember:
    """Gate for Admin-or-above actions — Owner is a superset of Admin, so it
    passes too. Owner-only actions (e.g. transferring ownership) enforce that
    narrower check themselves once they have the acting membership.
    """
    if membership.role not in (MemberRole.ADMIN, MemberRole.OWNER):
        raise PermissionDeniedError("Only household admins can perform this action")
    return membership


async def get_current_household(
    household_id: int,
    _membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
) -> Household:
    """Resolve the household entity itself, after confirming the caller is a member."""
    return await HouseholdService(db).get_household_or_404(household_id)
