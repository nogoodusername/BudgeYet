from datetime import datetime
from sqlalchemy import CheckConstraint, String, Integer, DateTime, ForeignKey, func
from sqlalchemy.orm import Mapped, mapped_column, relationship
import enum
from app.core.constants import HOUSEHOLD_MEMBER_CAP
from app.core.database import Base

class MemberRole(str, enum.Enum):
    ADMIN = "admin"
    MEMBER = "member"

class Household(Base):
    __tablename__ = "households"
    __table_args__ = (
        CheckConstraint(
            f"member_count >= 0 AND member_count <= {HOUSEHOLD_MEMBER_CAP}",
            name="ck_households_member_count_within_cap",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    currency: Mapped[str] = mapped_column(String(10), default="USD", nullable=False)
    language: Mapped[str] = mapped_column(String(10), default="en", nullable=False)
    cycle_start_day: Mapped[int] = mapped_column(Integer, default=1, nullable=False)
    # Denormalized counter, maintained atomically (see HouseholdRepository.try_reserve_member_slot/
    # release_member_slot) so the 3-member cap can be enforced as a single conditional UPDATE
    # instead of a check-then-act read+insert, which two concurrent joins could both pass.
    member_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now(), nullable=False
    )
    
    # Relationships
    members = relationship("HouseholdMember", back_populates="household", cascade="all, delete-orphan")
    budgets = relationship("Budget", back_populates="household", cascade="all, delete-orphan")
    categories = relationship("Category", back_populates="household", cascade="all, delete-orphan")
    transactions = relationship("Transaction", back_populates="household", cascade="all, delete-orphan")
    invites = relationship("Invite", back_populates="household", cascade="all, delete-orphan")


class HouseholdMember(Base):
    __tablename__ = "household_members"
    
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    household_id: Mapped[int] = mapped_column(ForeignKey("households.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), unique=True, index=True, nullable=False
    )
    role: Mapped[MemberRole] = mapped_column(String(20), default=MemberRole.MEMBER, nullable=False)
    joined_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), nullable=False)
    
    # Relationships
    household = relationship("Household", back_populates="members")
    user = relationship("User", back_populates="household_memberships")
