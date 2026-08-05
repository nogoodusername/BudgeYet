from typing import Optional

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user, get_db
from app.controllers import user_controller
from app.models.user import User
from app.schemas.household import HouseholdResponse
from app.schemas.user import UserResponse, UserUpdate

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return await user_controller.get_me(current_user)


@router.get("/me/household", response_model=Optional[HouseholdResponse])
async def get_my_household(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """The household the current user belongs to, or null if they haven't created/joined
    one yet. v1 caps a user to a single household. Neither /auth/login nor
    POST /households/join returns a household id directly, so this is how a client
    resolves it right after either."""
    return await user_controller.get_my_household(db, current_user)


@router.patch("/me", response_model=UserResponse)
async def update_me(
    payload: UserUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Editable: name, nickname, display mode. Email is permanently read-only."""
    return await user_controller.update_me(db, current_user, payload)
