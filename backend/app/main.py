from pathlib import Path
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from app.core.config import settings
from app.core.database import async_engine
from app.core.exceptions import (
    AppError,
    AuthenticationError,
    ConflictError,
    NotFoundError,
    PermissionDeniedError,
    RateLimitError,
    ValidationAppError,
)
from app.api.v1.endpoints import health
from app.api.v1.router import api_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    # Shutdown
    await async_engine.dispose()

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="FastAPI Backend for budge-yet Collaborative Household Budget App (OpenAPI 3.0)",
    version="0.1.0",
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

_ERROR_STATUS_CODES = {
    NotFoundError: 404,
    PermissionDeniedError: 403,
    AuthenticationError: 401,
    ConflictError: 409,
    RateLimitError: 429,
    ValidationAppError: 422,
}


def _register_exception_handlers(app: FastAPI) -> None:
    for exc_class, status_code in _ERROR_STATUS_CODES.items():

        def make_handler(code: int):
            async def handler(request: Request, exc: AppError):
                return JSONResponse(status_code=code, content={"detail": exc.message})

            return handler

        app.add_exception_handler(exc_class, make_handler(status_code))

    @app.exception_handler(AppError)
    async def fallback_app_error_handler(request: Request, exc: AppError):
        return JSONResponse(status_code=400, content={"detail": exc.message})


# Configure CORS for KMP/CMP Frontend targets (Android, iOS, Web)
# Android/iOS clients don't send an Origin header, so only the Web target is
# actually gated by this list. allow_credentials=True forbids a wildcard origin,
# so an explicit allowlist (settings.CORS_ORIGINS) is required.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS_LIST,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_register_exception_handlers(app)

# Root status route
@app.get("/", summary="Root API Welcome")
async def root():
    return {
        "message": "Welcome to budge-yet Household Budget API",
        "docs": "/docs",
        "health": "/health",
        "database": settings.DATABASE_TYPE,
    }

# Serve robots.txt to block crawlers on the API subdomain
@app.get("/robots.txt", summary="Robots Exclusion", include_in_schema=False)
async def robots_txt():
    return FileResponse(
        Path(__file__).parent / "static" / "robots.txt",
        media_type="text/plain",
    )

# Include API v1 routes
app.include_router(api_router, prefix=settings.API_V1_STR)
# Top-level health endpoint alias (unprefixed, for load balancers/uptime checks)
app.include_router(health.router)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
