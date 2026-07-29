from datetime import datetime, timedelta
from typing import Optional, Sequence

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.constants import HOUSEHOLD_MEMBER_CAP, INVITE_EXPIRY_DAYS
from app.core.email import send_invite_email
from app.core.exceptions import ConflictError, NotFoundError, ValidationAppError
from app.core.security import generate_invite_token
from app.models.household import Household, HouseholdMember, MemberRole
from app.models.invite import Invite
from app.models.user import User
from app.repositories.household_member_repository import HouseholdMemberRepository
from app.repositories.household_repository import HouseholdRepository
from app.repositories.invite_repository import InviteRepository
from app.schemas.household import HouseholdCreate, HouseholdUpdate, InviteCreate


class HouseholdService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.households = HouseholdRepository(db)
        self.members = HouseholdMemberRepository(db)
        self.invites = InviteRepository(db)

    async def create_household(self, user: User, payload: HouseholdCreate) -> Household:
        existing_membership = await self.members.get_by_user(user.id)
        if existing_membership is not None:
            raise ConflictError("You already belong to a household — v1 supports only one per user")

        household = await self.households.create(
            name=payload.name,
            currency=payload.currency,
            language=payload.language,
            cycle_start_day=payload.cycle_start_day,
        )
        await self.members.create(household_id=household.id, user_id=user.id, role=MemberRole.ADMIN)
        return await self.households.get_by_id(household.id)

    async def get_household_or_404(self, household_id: int) -> Household:
        household = await self.households.get_by_id(household_id)
        if household is None:
            raise NotFoundError("Household not found")
        return household

    async def update_household(self, household: Household, payload: HouseholdUpdate) -> Household:
        return await self.households.update(
            household,
            name=payload.name,
            currency=payload.currency,
            language=payload.language,
            cycle_start_day=payload.cycle_start_day,
        )

    async def create_invite(
        self, household: Household, invited_by: User, payload: InviteCreate
    ) -> Invite:
        member_count = await self.members.count_by_household(household.id)
        if member_count >= HOUSEHOLD_MEMBER_CAP:
            raise ConflictError(
                f"Household already has the maximum of {HOUSEHOLD_MEMBER_CAP} members"
            )

        invite = await self.invites.create(
            household_id=household.id,
            invited_by_id=invited_by.id,
            email=payload.email,
            token=generate_invite_token(),
            expires_at=datetime.utcnow() + timedelta(days=INVITE_EXPIRY_DAYS),
        )
        if payload.email:
            send_invite_email(payload.email, household.name, invite.token)
        return invite

    async def list_invites(self, household_id: int) -> Sequence[Invite]:
        return await self.invites.list_pending_by_household(household_id)

    async def revoke_invite(self, household_id: int, invite_id: int) -> None:
        invite = await self.invites.get_by_id(invite_id)
        if invite is None or invite.household_id != household_id:
            raise NotFoundError("Invite not found")
        if invite.revoked or invite.accepted_at is not None:
            raise ValidationAppError("Invite is no longer pending")
        await self.invites.revoke(invite)

    async def join_household(self, user: User, token: str) -> HouseholdMember:
        invite = await self.invites.get_by_token(token)
        if invite is None:
            raise NotFoundError("Invite not found")
        if invite.revoked or invite.accepted_at is not None:
            raise ValidationAppError("This invite is no longer valid")
        if invite.expires_at < datetime.utcnow():
            raise ValidationAppError("This invite has expired")

        existing_membership = await self.members.get_by_user(user.id)
        if existing_membership is not None:
            raise ConflictError("You already belong to a household — v1 supports only one per user")

        member_count = await self.members.count_by_household(invite.household_id)
        if member_count >= HOUSEHOLD_MEMBER_CAP:
            raise ConflictError(
                f"Household already has the maximum of {HOUSEHOLD_MEMBER_CAP} members"
            )

        membership = await self.members.create(
            household_id=invite.household_id, user_id=user.id, role=MemberRole.MEMBER
        )
        await self.invites.mark_accepted(invite)
        return membership

    async def remove_member(self, household_id: int, member_id: int) -> None:
        member = await self.members.get_by_id(member_id)
        if member is None or member.household_id != household_id:
            raise NotFoundError("Member not found")
        if member.role == MemberRole.ADMIN:
            admin_count = await self.members.count_admins(household_id)
            if admin_count <= 1:
                raise ConflictError("Cannot remove the household's only admin")
        await self.members.delete(member)

    async def leave_household(self, membership: HouseholdMember) -> None:
        if membership.role == MemberRole.ADMIN:
            admin_count = await self.members.count_admins(membership.household_id)
            if admin_count <= 1:
                raise ConflictError(
                    "Promote another member to admin before leaving — a household must keep an admin"
                )
        await self.members.delete(membership)

    async def update_member_role(
        self, household_id: int, member_id: int, new_role: MemberRole
    ) -> HouseholdMember:
        member = await self.members.get_by_id(member_id)
        if member is None or member.household_id != household_id:
            raise NotFoundError("Member not found")

        if member.role == MemberRole.ADMIN and new_role == MemberRole.MEMBER:
            admin_count = await self.members.count_admins(household_id)
            if admin_count <= 1:
                raise ConflictError("Cannot demote the household's only admin")

        return await self.members.update_role(member, new_role)

    async def get_membership_for_user(self, user_id: int) -> Optional[HouseholdMember]:
        return await self.members.get_by_user(user_id)
