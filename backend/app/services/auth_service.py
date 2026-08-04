from datetime import datetime, timedelta

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.email import send_pin_email
from app.core.exceptions import AuthenticationError, ConflictError, RateLimitError
from app.core.security import create_access_token, generate_pin, hash_pin, verify_pin
from app.models.user import User
from app.repositories.login_attempt_repository import LoginAttemptRepository
from app.repositories.user_repository import UserRepository
from app.schemas.user import UserCreate


class AuthService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.users = UserRepository(db)
        self.login_attempts = LoginAttemptRepository(db)

    async def signup(self, payload: UserCreate) -> User:
        existing = await self.users.get_by_email(payload.email)
        if existing is not None:
            raise ConflictError("An account with this email already exists")

        # PIN is user-chosen at signup (payload.pin), not server-generated — unlike forgot_pin
        # below, which still generates+emails one since that flow's whole point is recovering an
        # account the user is locked out of. No email is sent here: there's nothing to deliver
        # since the user already knows the PIN they just typed.
        user = await self.users.create(
            email=payload.email,
            full_name=payload.full_name,
            nickname=payload.nickname,
            pin_hash=hash_pin(payload.pin),
        )
        return user

    async def login(self, email: str, pin: str, ip_address: str) -> tuple[User, str]:
        now = datetime.utcnow()
        window_start = now - timedelta(minutes=settings.IP_LOCKOUT_WINDOW_MINUTES)
        recent_ip_failures = await self.login_attempts.count_recent_failures(
            ip_address, since=window_start
        )
        if recent_ip_failures >= settings.MAX_LOGIN_FAILURES_PER_IP:
            raise RateLimitError("Too many failed login attempts. Try again later.")

        user = await self.users.get_by_email(email)
        if user is None:
            await self.login_attempts.record_failure(ip_address)
            raise AuthenticationError("Invalid email or PIN")

        if user.locked_until is not None and user.locked_until > now:
            minutes_left = max(1, int((user.locked_until - now).total_seconds() // 60))
            raise AuthenticationError(
                f"Too many failed attempts. Try again in {minutes_left} minute(s)."
            )

        if not verify_pin(pin, user.pin_hash):
            locked_until = None
            if user.failed_login_attempts + 1 >= settings.MAX_LOGIN_ATTEMPTS:
                locked_until = now + timedelta(minutes=settings.LOGIN_LOCKOUT_MINUTES)
            await self.users.record_failed_login(user, locked_until=locked_until)
            await self.login_attempts.record_failure(ip_address)
            raise AuthenticationError("Invalid email or PIN")

        if user.failed_login_attempts or user.locked_until is not None:
            await self.users.reset_login_attempts(user)

        token = create_access_token(subject=user.id)
        return user, token

    async def forgot_pin(self, email: str) -> None:
        """Issue a fresh, server-generated PIN and email it — unlike signup, where the user
        chooses their own PIN, this flow has no other way to hand the user a working PIN.

        Silently no-ops for unknown emails so the endpoint can't be used to
        enumerate registered accounts.
        """
        user = await self.users.get_by_email(email)
        if user is None:
            return

        pin = generate_pin()
        await self.users.update(user, pin_hash=hash_pin(pin))
        await self.users.reset_login_attempts(user)
        send_pin_email(user.email, pin)
