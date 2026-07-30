from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from sqlalchemy import event, engine

from app.core.config import settings
from app.core.exceptions import AuthenticationError

class Base(DeclarativeBase):
    """Base declarative class for all SQLAlchemy ORM models."""
    pass

# Engine kwargs based on database type
engine_kwargs = {"echo": settings.DEBUG}

if settings.DATABASE_TYPE == "sqlite":
    # SQLite specific engine settings
    engine_kwargs["connect_args"] = {"check_same_thread": False}

async_engine = create_async_engine(settings.ASYNC_DATABASE_URI, **engine_kwargs)

# Enable Foreign Key support for SQLite
if settings.DATABASE_TYPE == "sqlite":
    @event.listens_for(engine.Engine, "connect")
    def set_sqlite_pragma(dbapi_connection, connection_record):
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.execute("PRAGMA journal_mode=WAL")
        cursor.close()

AsyncSessionLocal = async_sessionmaker(
    bind=async_engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)

async def get_async_db() -> AsyncGenerator[AsyncSession, None]:
    """Dependency provider yielding async SQLAlchemy database sessions.

    Commits once at the end of a request if the handler completed without raising,
    and rolls back otherwise — repositories/services only `flush()`, they never
    commit, so this is the single place a request's writes actually land.

    `AuthenticationError` is the one deliberate exception to "rolls back
    otherwise": login failure bookkeeping (e.g. the failed-attempt counter in
    `AuthService.login`) is flushed *before* that error is raised and must
    survive it, or an account could never actually get locked out. Every other
    error still rolls back in full.
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except AuthenticationError:
            await session.commit()
            raise
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
