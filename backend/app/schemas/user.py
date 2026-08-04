from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict, Field
from app.models.user import DisplayMode

class UserBase(BaseModel):
    email: EmailStr
    full_name: str
    nickname: str

class UserCreate(UserBase):
    # User-chosen at signup (not server-generated) — see AuthService.signup. Pattern enforces
    # exactly 6 digits since this is validated again by nothing else before hashing.
    pin: str = Field(pattern=r"^\d{6}$")

class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    nickname: Optional[str] = None
    display_mode: Optional[DisplayMode] = None

class UserResponse(UserBase):
    id: int
    display_mode: DisplayMode
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
