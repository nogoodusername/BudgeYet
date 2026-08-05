from typing import Optional

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.repositories.household_member_repository import HouseholdMemberRepository
from app.schemas.household import HouseholdResponse
from app.schemas.user import UserResponse, UserUpdate
from app.services.household_service import HouseholdService
from app.services.user_service import UserService


async def get_me(current_user: User) -> UserResponse:
    return UserResponse.model_validate(current_user)


async def update_me(db: AsyncSession, current_user: User, payload: UserUpdate) -> UserResponse:
    user = await UserService(db).update_profile(current_user, payload)
    return UserResponse.model_validate(user)


async def get_my_household(db: AsyncSession, current_user: User) -> Optional[HouseholdResponse]:
    """v1 caps a user to a single household (HouseholdMember.user_id is unique) — this is
    what a client calls right after login/join to learn whether one exists yet and, if so,
    its id, since neither the login nor join response carries it."""
    membership = await HouseholdMemberRepository(db).get_by_user(current_user.id)
    if membership is None:
        return None
    household = await HouseholdService(db).get_household_or_404(membership.household_id)
    return HouseholdResponse.model_validate(household)
