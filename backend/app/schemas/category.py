from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field

CategoryStatus = Literal["on_track", "warning", "over_budget"]

class CategoryBase(BaseModel):
    name: str
    icon: str = "default"
    monthly_limit: float = Field(ge=0, default=0.0)

class CategoryCreate(CategoryBase):
    pass

class CategoryUpdate(BaseModel):
    name: Optional[str] = None
    icon: Optional[str] = None
    monthly_limit: Optional[float] = Field(default=None, ge=0)

class CategoryResponse(CategoryBase):
    id: int
    household_id: int

    model_config = ConfigDict(from_attributes=True)

class CategoryWithStats(CategoryResponse):
    spent: float
    remaining: float
    percent_used: float
    status: CategoryStatus
