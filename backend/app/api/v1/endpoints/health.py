from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import text
from app.api.deps import get_db
from app.core.config import settings

router = APIRouter()

@router.get("/health", summary="Application & Database Health Check")
async def health_check(db: AsyncSession = Depends(get_db)):
    """
    Performs a database connectivity check and reports service readiness.
    """
    db_status = "unhealthy"
    try:
        result = await db.execute(text("SELECT 1"))
        if result.scalar() == 1:
            db_status = "healthy"
    except Exception as e:
        db_status = f"unhealthy: {str(e)}"
        
    return {
        "status": "online",
        "database_type": settings.DATABASE_TYPE,
        "database_status": db_status,
        "service": settings.PROJECT_NAME,
    }
