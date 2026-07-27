from datetime import datetime
from typing import List
from pydantic import BaseModel, ConfigDict
from app.models.household import MemberRole
from app.schemas.user import UserResponse

class HouseholdBase(BaseModel):
    name: str
    currency: str = "USD"
    cycle_start_day: int = 1

class HouseholdCreate(HouseholdBase):
    pass

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
