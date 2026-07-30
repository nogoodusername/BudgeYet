#!/usr/bin/env python3
"""
cleanup_login_failures.py
Purges login_failures rows older than settings.LOGIN_FAILURE_RETENTION_DAYS.

The table has no TTL of its own (see app/models/login_attempt.py), so without
this it grows forever. Intended to be run on a schedule (cron/systemd timer),
e.g.:
    0 3 * * * cd /path/to/backend && .venv/bin/python scripts/cleanup_login_failures.py
"""

import asyncio
import sys
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.config import settings
from app.core.database import AsyncSessionLocal
from app.repositories.login_attempt_repository import LoginAttemptRepository


async def cleanup_login_failures() -> int:
    cutoff = datetime.utcnow() - timedelta(days=settings.LOGIN_FAILURE_RETENTION_DAYS)
    async with AsyncSessionLocal() as session:
        deleted = await LoginAttemptRepository(session).delete_older_than(before=cutoff)
        await session.commit()
        return deleted


def main() -> None:
    deleted = asyncio.run(cleanup_login_failures())
    print(f"[cleanup_login_failures] deleted {deleted} row(s) older than "
          f"{settings.LOGIN_FAILURE_RETENTION_DAYS} day(s)")


if __name__ == "__main__":
    main()
