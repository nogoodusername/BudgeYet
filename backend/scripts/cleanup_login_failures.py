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
import logging
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.config import settings
from app.core.database import AsyncSessionLocal
from app.repositories.login_attempt_repository import LoginAttemptRepository

logger = logging.getLogger("budge_yet.cleanup_login_failures")
logger.setLevel(logging.INFO)
if not logger.handlers:
    # Own handler so run output is visible on stdout when this runs standalone
    # (cron/compose sidecar), regardless of any app-wide logging config.
    _handler = logging.StreamHandler()
    _handler.setFormatter(logging.Formatter("%(asctime)s %(name)s %(message)s"))
    logger.addHandler(_handler)
    logger.propagate = False


async def cleanup_login_failures() -> int:
    cutoff = datetime.utcnow() - timedelta(days=settings.LOGIN_FAILURE_RETENTION_DAYS)
    async with AsyncSessionLocal() as session:
        deleted = await LoginAttemptRepository(session).delete_older_than(before=cutoff)
        await session.commit()
        return deleted


def main() -> None:
    logger.info("run started (retention=%dd)", settings.LOGIN_FAILURE_RETENTION_DAYS)
    started = time.monotonic()
    try:
        deleted = asyncio.run(cleanup_login_failures())
    except Exception:
        logger.exception("run failed after %.2fs", time.monotonic() - started)
        raise
    logger.info("run finished in %.2fs, deleted %d row(s)", time.monotonic() - started, deleted)


if __name__ == "__main__":
    main()
