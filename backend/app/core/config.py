from typing import Literal
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "fam-ex Backend"
    DEBUG: bool = True
    API_V1_STR: str = "/api/v1"
    
    # Database Configuration Choice
    DATABASE_TYPE: Literal["sqlite", "postgres"] = "sqlite"
    
    # SQLite Options
    SQLITE_DB_FILE: str = "./data/fam_ex.db"
    
    # Postgres / Supabase / Aiven Options
    POSTGRES_SERVER: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "postgres"
    POSTGRES_DB: str = "fam_ex"
    POSTGRES_SSL: bool = False
    
    # Auth & Security
    SECRET_KEY: str = "change-this-super-secret-key-in-production"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days
    MAX_LOGIN_ATTEMPTS: int = 5
    LOGIN_LOCKOUT_MINUTES: int = 15
    MAX_LOGIN_FAILURES_PER_IP: int = 20
    IP_LOCKOUT_WINDOW_MINUTES: int = 15

    # CORS - comma-separated list of allowed origins (KMP/CMP Web target, local dev tools)
    CORS_ORIGINS: str = "http://localhost:8080,http://127.0.0.1:8080,http://localhost:3000"

    @property
    def CORS_ORIGINS_LIST(self) -> list[str]:
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    @property
    def ASYNC_DATABASE_URI(self) -> str:
        """Dynamically build async database connection URI based on DATABASE_TYPE."""
        if self.DATABASE_TYPE == "sqlite":
            # Uses aiosqlite for async SQLite operations
            return f"sqlite+aiosqlite:///{self.SQLITE_DB_FILE}"
        else:
            # Uses asyncpg driver for PostgreSQL (Supabase/Aiven/Local)
            ssl_option = "?ssl=require" if self.POSTGRES_SSL else ""
            return (
                f"postgresql+asyncpg://{self.POSTGRES_USER}:{self.POSTGRES_PASSWORD}"
                f"@{self.POSTGRES_SERVER}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}{ssl_option}"
            )
            
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()
