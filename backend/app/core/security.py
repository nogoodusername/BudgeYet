import secrets
import string
from datetime import datetime, timedelta, timezone
from typing import Any

import bcrypt
from fastapi import Request
from jose import JWTError, jwt

from app.core.config import settings

JWT_ALGORITHM = "HS256"


def get_client_ip(request: Request) -> str:
    """Resolve the real client IP, accounting for CDNs/load balancers (Cloudflare,
    AWS ALB/CloudFront, Azure Front Door/App Gateway, etc.) that terminate the
    connection themselves — `request.client.host` would otherwise return the
    proxy's IP rather than the actual client's, defeating IP-based rate limiting.
    """
    cf_connecting_ip = request.headers.get("CF-Connecting-IP")
    if cf_connecting_ip:
        return cf_connecting_ip.strip()

    real_ip = request.headers.get("X-Real-IP")
    if real_ip:
        return real_ip.strip()

    forwarded_for = request.headers.get("X-Forwarded-For")
    if forwarded_for:
        # Each proxy hop appends the address it received the request from, so the
        # last entry is the one added by our own (trusted) proxy/LB. Earlier
        # entries are client-supplied and therefore spoofable.
        last_hop = forwarded_for.split(",")[-1].strip()
        if last_hop:
            return last_hop

    return request.client.host if request.client else "unknown"


def generate_pin() -> str:
    """Generate a random 6-digit PIN, zero-padded."""
    return f"{secrets.randbelow(1_000_000):06d}"


def hash_pin(pin: str) -> str:
    return bcrypt.hashpw(pin.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_pin(pin: str, pin_hash: str) -> bool:
    return bcrypt.checkpw(pin.encode("utf-8"), pin_hash.encode("utf-8"))


_INVITE_TOKEN_ALPHABET = string.ascii_uppercase + string.digits
_INVITE_TOKEN_GROUP_LENGTH = 3
_INVITE_TOKEN_GROUP_COUNT = 3


def generate_invite_token() -> str:
    """Generate a household join code as XXX-XXX-XXX (uppercase alphanumeric) —
    short enough for a user to read aloud or retype by hand, unlike the raw
    urlsafe token this replaced.
    """
    groups = [
        "".join(secrets.choice(_INVITE_TOKEN_ALPHABET) for _ in range(_INVITE_TOKEN_GROUP_LENGTH))
        for _ in range(_INVITE_TOKEN_GROUP_COUNT)
    ]
    return "-".join(groups)


def create_access_token(subject: int, expires_delta: timedelta | None = None) -> str:
    expire = datetime.now(timezone.utc) + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    payload: dict[str, Any] = {"sub": str(subject), "exp": expire}
    return jwt.encode(payload, settings.SECRET_KEY, algorithm=JWT_ALGORITHM)


def decode_access_token(token: str) -> int:
    """Decode a JWT and return the user id. Raises JWTError if invalid/expired."""
    payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[JWT_ALGORITHM])
    subject = payload.get("sub")
    if subject is None:
        raise JWTError("Token missing subject")
    return int(subject)
