from datetime import datetime
from decimal import Decimal
from sqlalchemy import String, Numeric, Integer, DateTime, ForeignKey, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.core.database import Base

class Budget(Base):
    __tablename__ = "budgets"
    __table_args__ = (
        UniqueConstraint("household_id", "month", "year", name="uq_budgets_household_cycle"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    household_id: Mapped[int] = mapped_column(ForeignKey("households.id", ondelete="CASCADE"), nullable=False)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    monthly_goal_amount: Mapped[Decimal] = mapped_column(Numeric(12, 2), nullable=False)
    month: Mapped[int] = mapped_column(Integer, nullable=False)
    year: Mapped[int] = mapped_column(Integer, nullable=False)
    
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )
    
    # Relationships
    household = relationship("Household", back_populates="budgets")
