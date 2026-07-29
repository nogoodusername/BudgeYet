from sqlalchemy.ext.asyncio import AsyncSession

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
        if user is None or not verify_pin(pin, user.pin_hash):
            raise AuthenticationError("Invalid email or PIN")

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
        send_pin_email(user.email, pin)
