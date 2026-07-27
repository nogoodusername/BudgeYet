from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.database import async_engine, Base
from app.api.v1.router import api_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Create tables if using SQLite for simple local dev
    if settings.DATABASE_TYPE == "sqlite":
        async with async_engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
    yield
    # Shutdown
    await async_engine.dispose()

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="FastAPI Backend for fam-ex Collaborative Household Budget App (OpenAPI 3.0)",
    version="0.1.0",
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# Configure CORS for KMP/CMP Frontend targets (Android, iOS, Web)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Root status route
@app.get("/", summary="Root API Welcome")
async def root():
    return {
        "message": "Welcome to fam-ex Household Budget API",
        "docs": "/docs",
        "health": "/health",
        "database": settings.DATABASE_TYPE,
    }

# Include API v1 routes
app.include_router(api_router, prefix=settings.API_V1_STR)
# Top-level health endpoint alias
app.include_router(api_router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
