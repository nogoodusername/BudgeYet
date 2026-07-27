from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_async_db

async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """FastAPI dependency wrapper for database sessions."""
    async for session in get_async_db():
        yield session
