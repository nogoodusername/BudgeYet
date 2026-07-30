from datetime import datetime
from sqlalchemy import DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column
from app.core.database import Base


class LoginFailure(Base):
    """One row per failed login, used to throttle by source IP.

    Deliberately separate from `User.failed_login_attempts`: that counter is
    keyed by account and reset on success, which does nothing against an
    attacker spraying guesses across many emails (or many IPs) at one
    account. This table lets `AuthService.login` also cap failures per IP
    within a rolling window, independent of which account was targeted.
    """

    __tablename__ = "login_failures"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    ip_address: Mapped[str] = mapped_column(String(45), index=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), nullable=False, index=True
    )
