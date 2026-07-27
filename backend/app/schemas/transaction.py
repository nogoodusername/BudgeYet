from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict
from app.models.transaction import TransactionType, PaymentMode
from app.schemas.category import CategoryResponse
from app.schemas.user import UserResponse

class TransactionBase(BaseModel):
    amount: float
    merchant: str
    type: TransactionType = TransactionType.EXPENSE
    payment_mode: PaymentMode = PaymentMode.CARD
    notes: Optional[str] = None

class TransactionCreate(TransactionBase):
    household_id: int
    category_id: Optional[int] = None
    paid_by_id: int

class TransactionResponse(TransactionBase):
    id: int
    household_id: int
    category: Optional[CategoryResponse] = None
    paid_by_user: UserResponse
    transaction_date: datetime
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
