from datetime import datetime
from decimal import Decimal
from typing import Literal, Optional
from pydantic import BaseModel, ConfigDict, Field

BudgetStatus = Literal["on_track", "warning", "over_budget"]

class BudgetBase(BaseModel):
    name: str
    monthly_goal_amount: Decimal = Field(gt=0)

class BudgetCreate(BudgetBase):
    month: Optional[int] = Field(default=None, ge=1, le=12)
    year: Optional[int] = None

class BudgetUpdate(BaseModel):
    name: Optional[str] = None
    monthly_goal_amount: Optional[Decimal] = Field(default=None, gt=0)

class BudgetResponse(BudgetBase):
    id: int
    household_id: int
    month: int
    year: int
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

class BudgetWithStats(BudgetResponse):
    spent: Decimal
    remaining: Decimal
    percent_used: float
    status: BudgetStatus
