from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, EmailStr, ConfigDict
from app.models.household import MemberRole
from app.schemas.user import UserResponse

class HouseholdBase(BaseModel):
    name: str
    currency: str = "USD"
    language: str = "en"
    cycle_start_day: int = 1

class HouseholdCreate(HouseholdBase):
    pass

class HouseholdUpdate(BaseModel):
    name: Optional[str] = None
    currency: Optional[str] = None
    language: Optional[str] = None
    cycle_start_day: Optional[int] = None

class HouseholdMemberResponse(BaseModel):
    id: int
    user: UserResponse
    role: MemberRole
    joined_at: datetime

    model_config = ConfigDict(from_attributes=True)

class HouseholdResponse(HouseholdBase):
    id: int
    members: List[HouseholdMemberResponse] = []
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class MemberRoleUpdate(BaseModel):
    role: MemberRole

class InviteCreate(BaseModel):
    email: Optional[EmailStr] = None

class InviteResponse(BaseModel):
    id: int
    household_id: int
    email: Optional[str] = None
    expires_at: datetime
    accepted_at: Optional[datetime] = None
    revoked: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class JoinHouseholdRequest(BaseModel):
    token: str
