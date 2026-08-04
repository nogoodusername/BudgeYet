from typing import List

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import (
    get_current_household,
    get_current_user,
    get_db,
    get_household_membership,
    require_admin_membership,
)
from app.controllers import household_controller
from app.models.household import Household, HouseholdMember
from app.models.user import User
from app.schemas.household import (
    HouseholdCreate,
    HouseholdMemberResponse,
    HouseholdResponse,
    HouseholdUpdate,
    InviteCreate,
    InviteResponse,
    JoinHouseholdRequest,
    MemberRoleUpdate,
)

router = APIRouter(prefix="/households", tags=["Households"])


@router.post("", response_model=HouseholdResponse, status_code=status.HTTP_201_CREATED)
async def create_household(
    payload: HouseholdCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Create a household. The creator becomes its Owner."""
    return await household_controller.create_household(db, current_user, payload)


@router.post("/join", response_model=HouseholdMemberResponse, status_code=status.HTTP_201_CREATED)
async def join_household(
    payload: JoinHouseholdRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """Join a household via an invite token. Joins as a Member."""
    return await household_controller.join_household(db, current_user, payload)


@router.get("/{household_id}", response_model=HouseholdResponse)
async def get_household(
    household: Household = Depends(get_current_household),
):
    return HouseholdResponse.model_validate(household)


@router.patch("/{household_id}", response_model=HouseholdResponse)
async def update_household(
    household_id: int,
    payload: HouseholdUpdate,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Update name/currency/language/cycle start day. Admin only."""
    return await household_controller.update_household(db, household_id, payload)


@router.post(
    "/{household_id}/leave", status_code=status.HTTP_204_NO_CONTENT, response_model=None
)
async def leave_household(
    membership: HouseholdMember = Depends(get_household_membership),
    db: AsyncSession = Depends(get_db),
):
    """Leave the household. The Owner must transfer ownership first."""
    await household_controller.leave_household(db, membership)


@router.post(
    "/{household_id}/invites", response_model=InviteResponse, status_code=status.HTTP_201_CREATED
)
async def create_invite(
    household_id: int,
    payload: InviteCreate,
    admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Create a household invite (expires in 7 days). Admin only. Blocked once the
    3-member cap is reached."""
    return await household_controller.create_invite(db, household_id, admin.user, payload)


@router.get("/{household_id}/invites", response_model=List[InviteResponse])
async def list_invites(
    household_id: int,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """List pending (unaccepted, unrevoked) invites. Admin only."""
    return await household_controller.list_invites(db, household_id)


@router.delete(
    "/{household_id}/invites/{invite_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    response_model=None,
)
async def revoke_invite(
    household_id: int,
    invite_id: int,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Revoke a pending invite. Admin only."""
    await household_controller.revoke_invite(db, household_id, invite_id)


@router.delete(
    "/{household_id}/members/{member_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    response_model=None,
)
async def remove_member(
    household_id: int,
    member_id: int,
    _admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Remove a member. Admin or Owner. The Owner cannot be removed — transfer ownership first."""
    await household_controller.remove_member(db, household_id, member_id)


@router.patch("/{household_id}/members/{member_id}/role", response_model=HouseholdMemberResponse)
async def update_member_role(
    household_id: int,
    member_id: int,
    payload: MemberRoleUpdate,
    admin: HouseholdMember = Depends(require_admin_membership),
    db: AsyncSession = Depends(get_db),
):
    """Promote/demote a member. Admin or Owner. Transferring ownership (role=owner)
    is Owner-only and requires the target to already be an Admin; the household
    always keeps exactly one Owner."""
    return await household_controller.update_member_role(db, household_id, member_id, payload, admin)
