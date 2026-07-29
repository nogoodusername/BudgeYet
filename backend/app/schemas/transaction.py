from datetime import date, datetime
from decimal import Decimal
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field, model_validator
from app.models.transaction import TransactionType, PaymentMode
from app.schemas.category import CategoryResponse
from app.schemas.user import UserResponse

class TransactionBase(BaseModel):
    amount: Decimal = Field(gt=0)
    merchant: str
    type: TransactionType = TransactionType.EXPENSE
    payment_mode: PaymentMode = PaymentMode.CARD
    notes: Optional[str] = None
    transaction_date: Optional[datetime] = None

class TransactionCreate(TransactionBase):
    category_id: Optional[int] = None
    paid_by_id: Optional[int] = None

    @model_validator(mode="after")
    def category_required_for_expense(self):
        if self.type == TransactionType.EXPENSE and self.category_id is None:
            raise ValueError("category_id is required for expense transactions")
        return self

class TransactionUpdate(BaseModel):
    amount: Optional[Decimal] = Field(default=None, gt=0)
    merchant: Optional[str] = None
    type: Optional[TransactionType] = None
    category_id: Optional[int] = None
    paid_by_id: Optional[int] = None
    payment_mode: Optional[PaymentMode] = None
    notes: Optional[str] = None
    transaction_date: Optional[datetime] = None

class TransactionResponse(TransactionBase):
    id: int
    household_id: int
    category: Optional[CategoryResponse] = None
    paid_by_user: UserResponse
    created_by_user: UserResponse
    transaction_date: datetime
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class TransactionFilterParams(BaseModel):
    category_id: Optional[int] = None
    paid_by_id: Optional[int] = None
    type: Optional[TransactionType] = None
    payment_mode: Optional[PaymentMode] = None
    date_from: Optional[date] = None
    date_to: Optional[date] = None
    search: Optional[str] = None
    amount_min: Optional[float] = Field(default=None, ge=0)
    amount_max: Optional[float] = Field(default=None, ge=0)
    limit: int = Field(default=50, ge=1, le=200)
    offset: int = Field(default=0, ge=0)
