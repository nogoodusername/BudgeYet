from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict
from app.models.transaction import TransactionType
from app.schemas.budget import BudgetWithStats
from app.schemas.category import CategoryWithStats
from app.schemas.user import UserResponse

class DashboardResponse(BaseModel):
    has_budget: bool
    has_transactions: bool
    budget: Optional[BudgetWithStats] = None
    categories: List[CategoryWithStats] = []

class ActivityFeedItem(BaseModel):
    id: int
    type: TransactionType
    amount: float
    merchant: str
    category_name: Optional[str] = None
    user: UserResponse
    transaction_date: datetime
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
