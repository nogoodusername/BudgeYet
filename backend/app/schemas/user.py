from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict
from app.models.user import DisplayMode

class UserBase(BaseModel):
    email: EmailStr
    full_name: str
    nickname: str

class UserCreate(UserBase):
    pass

class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None
    display_mode: Optional[DisplayMode] = None

class UserResponse(UserBase):
    id: int
    avatar_url: Optional[str] = None
    display_mode: DisplayMode
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
