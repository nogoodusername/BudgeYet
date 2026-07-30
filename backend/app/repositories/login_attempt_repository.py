from datetime import datetime

from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.login_attempt import LoginFailure


class LoginAttemptRepository:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def count_recent_failures(self, ip_address: str, *, since: datetime) -> int:
        result = await self.db.execute(
            select(func.count())
            .select_from(LoginFailure)
            .where(LoginFailure.ip_address == ip_address, LoginFailure.created_at >= since)
        )
        return result.scalar_one()

    async def record_failure(self, ip_address: str) -> None:
        self.db.add(LoginFailure(ip_address=ip_address))
        await self.db.flush()

    async def delete_older_than(self, *, before: datetime) -> int:
        """Purge failure rows older than `before`. Used by the retention cleanup job."""
        result = await self.db.execute(delete(LoginFailure).where(LoginFailure.created_at < before))
        return result.rowcount
