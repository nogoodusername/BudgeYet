from decimal import Decimal
from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field

CategoryStatus = Literal["on_track", "warning", "over_budget"]

class CategoryBase(BaseModel):
    name: str
    icon: str = "default"
    monthly_limit: Decimal = Field(ge=0, default=Decimal("0"))

class CategoryCreate(CategoryBase):
    pass

class CategoryUpdate(BaseModel):
    name: Optional[str] = None
    icon: Optional[str] = None
    monthly_limit: Optional[Decimal] = Field(default=None, ge=0)

class CategoryResponse(CategoryBase):
    id: int
    household_id: int

    model_config = ConfigDict(from_attributes=True)

class CategoryWithStats(CategoryResponse):
    spent: Decimal
    remaining: Decimal
    percent_used: float
    status: CategoryStatus
