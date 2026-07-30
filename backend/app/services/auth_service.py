from datetime import datetime, timedelta

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.email import send_pin_email
from app.core.exceptions import AuthenticationError, ConflictError
from app.core.security import create_access_token, generate_pin, hash_pin, verify_pin
from app.models.user import User
from app.repositories.user_repository import UserRepository
from app.schemas.user import UserCreate


class AuthService:
    def __init__(self, db: AsyncSession):
        self.db = db
        self.users = UserRepository(db)

    async def signup(self, payload: UserCreate) -> User:
        existing = await self.users.get_by_email(payload.email)
        if existing is not None:
            raise ConflictError("An account with this email already exists")

        pin = generate_pin()
        user = await self.users.create(
            email=payload.email,
            full_name=payload.full_name,
            nickname=payload.nickname,
            pin_hash=hash_pin(pin),
        )
        send_pin_email(user.email, pin)
        return user

    async def login(self, email: str, pin: str) -> tuple[User, str]:
        user = await self.users.get_by_email(email)
        if user is None:
            raise AuthenticationError("Invalid email or PIN")

        now = datetime.utcnow()
        if user.locked_until is not None and user.locked_until > now:
            minutes_left = max(1, (user.locked_until - now).seconds // 60)
            raise AuthenticationError(
                f"Too many failed attempts. Try again in {minutes_left} minute(s)."
            )

        if not verify_pin(pin, user.pin_hash):
            locked_until = None
            if user.failed_login_attempts + 1 >= settings.MAX_LOGIN_ATTEMPTS:
                locked_until = now + timedelta(minutes=settings.LOGIN_LOCKOUT_MINUTES)
            await self.users.record_failed_login(user, locked_until=locked_until)
            raise AuthenticationError("Invalid email or PIN")

        if user.failed_login_attempts or user.locked_until is not None:
            await self.users.reset_login_attempts(user)

        token = create_access_token(subject=user.id)
        return user, token

    async def forgot_pin(self, email: str) -> None:
        """Issue a fresh PIN and email it, mirroring the signup flow.

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
