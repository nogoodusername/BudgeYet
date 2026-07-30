from datetime import datetime
from decimal import Decimal
from sqlalchemy import String, Numeric, DateTime, ForeignKey, Index, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum
from app.core.database import Base

class TransactionType(str, enum.Enum):
    EXPENSE = "expense"
    INCOME = "income"

class PaymentMode(str, enum.Enum):
    CASH = "cash"
    CARD = "card"
    BANK_TRANSFER = "bank_transfer"
    OTHER = "other"

class Transaction(Base):
    __tablename__ = "transactions"
    __table_args__ = (
        Index("ix_transactions_household_id_transaction_date", "household_id", "transaction_date"),
        Index("ix_transactions_household_id_created_at", "household_id", "created_at"),
        Index("ix_transactions_category_id", "category_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    household_id: Mapped[int] = mapped_column(ForeignKey("households.id", ondelete="CASCADE"), nullable=False)
    category_id: Mapped[int] = mapped_column(ForeignKey("categories.id", ondelete="SET NULL"), nullable=True)
    paid_by_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    created_by_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    type: Mapped[TransactionType] = mapped_column(String(20), default=TransactionType.EXPENSE, nullable=False)
    amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    merchant: Mapped[str] = mapped_column(String(255), nullable=False)
    payment_mode: Mapped[PaymentMode] = mapped_column(String(50), default=PaymentMode.CARD, nullable=False)
    notes: Mapped[str] = mapped_column(String(500), nullable=True)
    receipt_url: Mapped[str] = mapped_column(String(500), nullable=True)
    
    transaction_date: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    
    # Relationships
    household = relationship("Household", back_populates="transactions")
    category = relationship("Category", back_populates="transactions")
    paid_by_user = relationship("User", foreign_keys=[paid_by_id], back_populates="transactions")
    created_by_user = relationship("User", foreign_keys=[created_by_id])
