from typing import Sequence

from sqlalchemy.ext.asyncio import AsyncSession

from app.models.household import HouseholdMember
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
from app.services.household_service import HouseholdService


async def create_household(
    db: AsyncSession, current_user: User, payload: HouseholdCreate
) -> HouseholdResponse:
    household = await HouseholdService(db).create_household(current_user, payload)
    return HouseholdResponse.model_validate(household)


async def get_household(db: AsyncSession, household_id: int) -> HouseholdResponse:
    household = await HouseholdService(db).get_household_or_404(household_id)
    return HouseholdResponse.model_validate(household)


async def update_household(
    db: AsyncSession, household_id: int, payload: HouseholdUpdate
) -> HouseholdResponse:
    service = HouseholdService(db)
    household = await service.get_household_or_404(household_id)
    updated = await service.update_household(household, payload)
    return HouseholdResponse.model_validate(updated)


async def create_invite(
    db: AsyncSession, household_id: int, current_user: User, payload: InviteCreate
) -> InviteResponse:
    service = HouseholdService(db)
    household = await service.get_household_or_404(household_id)
    invite = await service.create_invite(household, current_user, payload)
    return InviteResponse.model_validate(invite)


async def list_invites(db: AsyncSession, household_id: int) -> Sequence[InviteResponse]:
    invites = await HouseholdService(db).list_invites(household_id)
    return [InviteResponse.model_validate(i) for i in invites]


async def revoke_invite(db: AsyncSession, household_id: int, invite_id: int) -> None:
    await HouseholdService(db).revoke_invite(household_id, invite_id)


async def join_household(
    db: AsyncSession, current_user: User, payload: JoinHouseholdRequest
) -> HouseholdMemberResponse:
    membership = await HouseholdService(db).join_household(current_user, payload.token)
    return HouseholdMemberResponse.model_validate(membership)


async def remove_member(db: AsyncSession, household_id: int, member_id: int) -> None:
    await HouseholdService(db).remove_member(household_id, member_id)


async def leave_household(db: AsyncSession, membership: HouseholdMember) -> None:
    await HouseholdService(db).leave_household(membership)


async def update_member_role(
    db: AsyncSession,
    household_id: int,
    member_id: int,
    payload: MemberRoleUpdate,
    acting_membership: HouseholdMember,
) -> HouseholdMemberResponse:
    member = await HouseholdService(db).update_member_role(
        household_id, member_id, payload.role, acting_membership
    )
    return HouseholdMemberResponse.model_validate(member)
