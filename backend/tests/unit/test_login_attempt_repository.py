from datetime import datetime, timedelta

import pytest

from app.models.login_attempt import LoginFailure
from app.repositories.login_attempt_repository import LoginAttemptRepository
from tests.conftest import TestSessionLocal

pytestmark = pytest.mark.asyncio


async def test_delete_older_than_purges_only_stale_rows():
    now = datetime.utcnow()
    async with TestSessionLocal() as session:
        session.add_all(
            [
                LoginFailure(ip_address="1.1.1.1", created_at=now - timedelta(days=40)),
                LoginFailure(ip_address="1.1.1.1", created_at=now - timedelta(days=31)),
                LoginFailure(ip_address="1.1.1.1", created_at=now - timedelta(days=1)),
            ]
        )
        await session.commit()

        repo = LoginAttemptRepository(session)
        deleted = await repo.delete_older_than(before=now - timedelta(days=30))
        await session.commit()

        assert deleted == 2
        remaining = await repo.count_recent_failures("1.1.1.1", since=now - timedelta(days=365))
        assert remaining == 1
