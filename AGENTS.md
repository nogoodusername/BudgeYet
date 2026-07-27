# AGENTS.md — fam-ex

Instructions for AI coding agents working in this repository. Read this before making changes.

## What this is

`fam-ex` is a collaborative household budget app (v1/MVP, currently early scaffolding). Households share a
single monthly budget with categories, limits, and a transaction ledger; members see each other's spending
in real time. Full product intent lives in [docs/household-budget-app-prd.md](docs/household-budget-app-prd.md)
— **read it before implementing any feature**, since business rules (roles, limits, rollover behavior) are
specific and easy to get wrong by guessing. Technical design lives in [docs/architecture.md](docs/architecture.md).

Monorepo with two independently-built projects:

```
fam-ex/
├── backend/     FastAPI (Python 3.11+) REST API
├── frontend/    Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Web/Wasm)
└── docs/        PRD + architecture spec — source of truth for product behavior
```

Backend and frontend have separate CI pipelines gated by path (`backend/**`, `frontend/**`) — see
`.github/workflows/`. Only touch the toolchain relevant to the files you're changing.

## Current state (important)

This is a fresh scaffold, not a mature codebase — most PRD functionality is **not yet implemented**:
- Backend: only `health` endpoint exists under `app/api/v1/endpoints/`. Models exist for
  `users`, `households`, `budgets`, `categories`, `transactions` but most have no corresponding
  endpoints, services, or Alembic migrations yet. No `backend/tests/` directory exists yet even though
  `pyproject.toml` points `pytest` at `testpaths = ["tests"]` — create it when you add the first test.
- Frontend: only a single `DashboardScreen.kt` composable and shared `Models.kt` exist in `commonMain`.
  No networking, navigation, or auth flow wired up yet.
- No auth/JWT issuance is implemented despite `SECRET_KEY` / `ACCESS_TOKEN_EXPIRE_MINUTES` config existing.

Don't assume a feature exists because it's in the PRD or in a model/schema — check the actual endpoint
router and frontend screens first.

## Backend (`backend/`)

**Stack:** FastAPI + Pydantic v2 + SQLAlchemy 2.0 async ORM + Alembic. SQLite (`aiosqlite`) or Postgres
(`asyncpg`) selected via `DATABASE_TYPE` env var — code must stay driver-agnostic (no SQLite- or
Postgres-only SQL/features) since both are supported deployment targets.

**Layout convention** (`app/`):
- `models/` — SQLAlchemy ORM classes (one file per aggregate: `user.py`, `household.py`, `budget.py`,
  `category.py`, `transaction.py`). Declare `Mapped[...]`/`mapped_column` style, not legacy `Column`.
- `schemas/` — Pydantic request/response models, mirroring `models/` filenames. Follow the existing
  `XBase` / `XCreate` / `XResponse` naming split (see `schemas/user.py`).
- `api/v1/endpoints/` — one router module per resource; registered in `api/v1/router.py`.
  New endpoints must be added to that router, not mounted ad hoc in `main.py`.
- `api/deps.py` — shared FastAPI dependencies (DB session, auth, etc.).
- `core/config.py` — `Settings` (pydantic-settings), loaded from `.env`. Add new config here, not as
  scattered `os.environ` reads.
- `core/database.py` — async engine/session setup and `Base`.

**Commands:**
```bash
cd backend
python3 scripts/setup_env.py sqlite   # or: postgres — generates .env non-interactively
pip install -e ".[dev]"
uvicorn app.main:app --reload --port 8000
ruff check app/                       # lint — CI runs this, must be clean
pytest -v                             # CI runs this; create backend/tests/ if it doesn't exist
```
Swagger UI at `/docs`, health check at `/health`. Docker: `docker-compose up --build -d` (Postgres) or
`docker-compose -f docker-compose.sqlite.yml up --build -d` (SQLite).

**Conventions:**
- All route handlers are `async def`; use the async session from `api/deps.get_db`, never a sync session.
- Integer autoincrement PKs (see `models/user.py`), not UUIDs — stay consistent with existing models.
- `created_at`/`updated_at` via `server_default=func.now()` / `onupdate=func.now()` on every table.
- New DB schema changes need an Alembic migration (`alembic/`) — don't rely on `Base.metadata.create_all`
  outside of the SQLite dev-convenience path in `main.py`'s lifespan.
- Run `ruff check app/` before considering backend work done; CI will fail otherwise.

## Frontend (`frontend/`)

**Stack:** Kotlin Multiplatform + Compose Multiplatform targeting Android, iOS, and Web (Wasm/JS), with
Ktor as the HTTP client. Package root is `com.famex`.

**Source sets** (under `composeApp/src/`):
- `commonMain/` — shared UI (Compose), state, domain models, networking. Put new feature code here by
  default; only drop into a platform-specific source set for genuine platform APIs.
- `androidMain/` — `MainActivity`, manifest, Android-only integrations.
- `iosMain/` — `MainViewController` bridge consumed by the Xcode wrapper in `iosApp/`.
- `wasmJsMain/` — browser entrypoint (`main.kt`) and `index.html`.

**Commands:**
```bash
cd frontend
./gradlew :composeApp:assembleDebug                          # Android
./gradlew :composeApp:wasmJsBrowserDevelopmentRun             # Web (serves at localhost:8080)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode      # iOS framework (then open iosApp/iosApp.xcodeproj in Xcode)
```
Toolchain: Kotlin 1.9.23, Compose Multiplatform 1.6.1, Ktor 2.3.9, AGP 8.2.2, JDK 17. Versions are pinned
centrally in `gradle/libs.versions.toml` — add new dependencies there, referenced via the version catalog
(`libs.xxx`), not as inline coordinate strings in `build.gradle.kts`.

**Design system — "Stability & Growth"** (`theme/Color.kt`, `theme/Theme.kt`): Manrope typeface; Slate 900
(`#0f172a`) base; Teal `#0d9488` = on-track/positive; Amber `#d97706` = 75–99% of a budget/limit used;
Coral `#e11d48` = at/over 100% (over-budget warning). 8px rounded corners, card-based lists, persistent
bottom nav + FAB. Reuse these tokens for any new UI — don't hardcode new colors ad hoc.

## Key business rules to respect (from the PRD)

These affect any backend logic or UI you write around budgets/transactions — get them from the PRD, not
assumptions:
- Roles are **Admin / Member** only. Members can add/edit/delete only their *own* transactions; Admins can
  edit/delete anyone's. Only Admins manage categories, limits, invites, and household currency/language.
- One budget per household, one currency per household (not per-transaction).
- Category limits **reset every cycle with no rollover** — but historical transactions/snapshots for prior
  cycles must remain intact and queryable by date range.
- Household hard cap: **3 members** (including Admin) in v1.
- Future-dated transactions are **disallowed**.
- Auth is email + 6-digit PIN (emailed at signup), not password-based.
- Invite links expire after **7 days**.
- Status thresholds are consistent across dashboard and category views: teal < 75%, amber 75–99%,
  coral/red ≥ 100%.

## CI expectations

- `backend-ci.yml`: `ruff check app/`, `pytest -v` (against a fresh SQLite env via `setup_env.py sqlite`),
  then a Docker build. Keep backend changes lint-clean and test-covered.
- `frontend-ci.yml`: validates Gradle build graph for Android, Web Wasm, and iOS framework compile targets
  on every PR touching `frontend/**`.

Only run/validate the pipeline(s) relevant to the code you changed.
