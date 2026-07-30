# AGENTS.md — fam-ex

Instructions for AI coding agents working in this repository. Read this before making changes. Update the AGENTS.md regularly to reflect new reality.

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

- Backend: v1 MVP REST surface is implemented — auth (email + 6-digit PIN, JWT, forgot-PIN
  reissue), households (create/update, invites, join, member roles, leave/remove), budgets,
  categories (with reassign-before-delete), transactions (role-scoped edit/delete, filterable by
  category/payer/type/payment mode/date range/amount range/merchant-or-category search), dashboard,
  and a polling activity feed. Follows a Router → Controller → Service → Repository layering under
  `app/` (`api/v1/endpoints/` → `controllers/` → `services/` → `repositories/`). `alembic/versions/`
  now holds a full migration chain (not a single initial migration) — schema has evolved
  incrementally (login lockout fields, per-IP login-failure table, denormalized/atomic household
  member counter, unique constraints, Numeric money columns, indexes) since the original v1 cut; see
  "Auth hardening" below and the Alembic note under Backend conventions before assuming the schema
  matches `e2eaf9009180_initial_schema.py` alone. `backend/tests/` has unit tests (`tests/unit/`) for
  `core/security.py` and `services/cycle_utils.py`, and integration tests (`tests/integration/`)
  per resource group hitting the API via `httpx.AsyncClient` against an in-memory SQLite DB.
- **Auth hardening (since v1):** login now has two independent throttles — a per-account lockout
  (`User.failed_login_attempts`/`locked_until`, tunable via `MAX_LOGIN_ATTEMPTS`/
  `LOGIN_LOCKOUT_MINUTES` in `core/config.py`) and a per-IP rate limit backed by the `login_failures`
  table (`models/login_attempt.py`, `repositories/login_attempt_repository.py`), tunable via
  `MAX_LOGIN_FAILURES_PER_IP`/`IP_LOCKOUT_WINDOW_MINUTES`. The IP throttle exists specifically to
  catch an attacker spraying guesses across many accounts, which the per-account counter alone can't
  see. Both live in `AuthService.login` (`services/auth_service.py`) and raise `RateLimitError`
  (→ HTTP 429) or `AuthenticationError` (→ HTTP 401, and see the commit carve-out on
  `AuthenticationError` under `core/database.py` below). Money amounts (`Transaction.amount`,
  `Budget.monthly_goal_amount`) are `Numeric(12, 2)`/`Decimal`, not `Float` — keep new money columns
  consistent with that.
- Frontend: still only a single `DashboardScreen.kt` composable and shared `Models.kt` exist in
  `commonMain`. No networking, navigation, or auth flow wired up yet — the backend above is ready
  for it to consume. When the networking layer lands, the backend base URL must be user-configurable
  at onboarding (PRD A0/Section 9.9): default to our hosted backend, but let the user point the app
  at their own self-hosted deployment instead. Store it as a device-level setting (not per-household)
  and don't hardcode the hosted URL as the only option.

Don't assume a feature exists because it's in the PRD or in a model/schema — check the actual endpoint
router and frontend screens first.

### Known gaps (deliberately deferred, see PR discussion)

- **Email delivery is a stub.** `app/core/email.py` logs PIN and invite messages instead of sending
  them (no SMTP/SES integration yet). Signup/login PINs and invite tokens are **not** echoed back in
  any API response — until real delivery is wired up, retrieving them requires reading server logs or
  querying the DB directly (see how `backend/tests/helpers.py` does it for tests, by monkeypatching the
  generators). Real delivery must land before this is usable outside local dev.
- **Real-time activity feed is REST-only.** `GET /households/{id}/activity-feed` is polled, not pushed.
  The PRD's WebSocket/live-push behavior (B4) was explicitly deferred to a follow-up.
- **Receipt photo upload is fully out of scope**, backend and frontend. `Transaction.receipt_url`
  exists on the model but there is no upload endpoint or storage integration, and no client-side
  capture flow either.
- **Per-IP login rate limit is bypassable on a bare deployment.** `get_client_ip` (`core/security.py`)
  unconditionally trusts `CF-Connecting-IP`/`X-Real-IP`/`X-Forwarded-For` from the incoming request,
  with no check that the request actually passed through a trusted proxy that sets/overwrites those
  headers. This is safe behind a CDN that strips client-supplied versions of them (Cloudflare, etc.),
  but on the installer's default path (`install.sh` → `docker-compose up` on a bare VPS, no CDN
  required) any client can spoof a different `X-Forwarded-For` on every request and evade
  `MAX_LOGIN_FAILURES_PER_IP` entirely — the per-account lockout (`User.failed_login_attempts`) still
  holds, but the per-IP throttle doesn't. Fix is to gate header-trust behind an explicit
  `TRUSTED_PROXY_COUNT`/`BEHIND_PROXY` setting (off by default), falling back to `request.client.host`
  when unset — see `core/security.py`.

## Backend (`backend/`)

**Stack:** FastAPI + Pydantic v2 + SQLAlchemy 2.0 async ORM + Alembic. SQLite (`aiosqlite`) or Postgres
(`asyncpg`) selected via `DATABASE_TYPE` env var — code must stay driver-agnostic (no SQLite- or
Postgres-only SQL/features) since both are supported deployment targets.

**One-command server installer:** [`scripts/install.sh`](scripts/install.sh) (top-level, not
`backend/scripts/`) is a standalone `curl | bash`-able installer for deploying the backend on a fresh
server — clones the repo, drives `backend/scripts/setup_env.py` for DB setup, and runs the right
`docker-compose*.yml` file. It shells out to `setup_env.py sqlite|postgres` and reads
`POSTGRES_USER`/`POSTGRES_PASSWORD`/`POSTGRES_DB`/`DATABASE_TYPE` back out of the generated `.env`, so
if you change that script's CLI (arg names, non-interactive env var names) or either compose file's
name/service names/port mapping, update `install.sh` to match — nothing type-checks that coupling.
It also patches a `COMPOSE_PROJECT_NAME` into `.env` (compose otherwise derives it from the `backend/`
dirname, which collides across separate installs on one host) — don't remove that without accounting
for the collision it fixes.

**Layout convention** (`app/`) — strict Router → Controller → Service → Repository layering:
- `models/` — SQLAlchemy ORM classes (one file per aggregate: `user.py`, `household.py` (also holds
  `HouseholdMember`), `invite.py`, `budget.py`, `category.py`, `transaction.py`, `login_attempt.py`
  (holds `LoginFailure`, the per-IP login-throttle table — see "Auth hardening" above)). Declare
  `Mapped[...]`/`mapped_column` style, not legacy `Column`.
- `schemas/` — Pydantic request/response models, mirroring `models/` filenames. Follow the existing
  `XBase` / `XCreate` / `XUpdate` / `XResponse` naming split (see `schemas/user.py`). `common.py` has
  the generic `Page[T]` pagination envelope.
- `api/v1/endpoints/` — one router module per resource; registered in `api/v1/router.py`. Routers only
  do HTTP concerns (path/query/body parsing, status codes, `Depends`) and call into `controllers/` —
  they never touch a repository or session-scoped business rule directly.
- `api/deps.py` — shared FastAPI dependencies: `get_db`, `get_current_user` (JWT bearer), and household
  access-control deps (`get_household_membership`, `require_admin_membership`, `get_current_household`).
- `controllers/` — thin orchestration between routers and services; maps request schemas to service
  calls and service results back to response schemas. No SQLAlchemy imports here.
- `services/` — business rules (role permissions, the 3-member cap, future-date rejection, cycle
  math in `cycle_utils.py`, delete-blocked-by-transactions, invite expiry, "always one admin",
  login lockout/rate-limit — see "Auth hardening" above). Raises the domain exceptions in
  `core/exceptions.py` (`NotFoundError`, `ConflictError`, `PermissionDeniedError`,
  `ValidationAppError`, `AuthenticationError`, `RateLimitError`) — never `fastapi.HTTPException`
  directly. `main.py` registers exception handlers that translate these to HTTP responses
  (`_ERROR_STATUS_CODES`; anything else raised as a bare `AppError` falls back to 400).
- `repositories/` — the only layer that touches `AsyncSession`/SQLAlchemy queries directly. No business
  logic — just CRUD and filtered/aggregated reads. Includes `login_attempt_repository.py` (per-IP
  failure counting/recording). Race-condition-prone invariants are enforced as atomic DB operations
  here rather than check-then-act service code: the 3-member household cap is a conditional `UPDATE`
  on `Household.member_count` (`HouseholdRepository.try_reserve_member_slot`/`release_member_slot`,
  backed by a `CheckConstraint`), one-household-per-user is a `unique=True` on
  `HouseholdMember.user_id`, and one-budget-per-household-per-cycle is a `UniqueConstraint` on
  `Budget(household_id, month, year)` — don't reintroduce a read-then-insert check for any of these.
- `core/config.py` — `Settings` (pydantic-settings), loaded from `.env`. Add new config here, not as
  scattered `os.environ` reads. Includes login-throttle knobs (`MAX_LOGIN_ATTEMPTS`,
  `LOGIN_LOCKOUT_MINUTES`, `MAX_LOGIN_FAILURES_PER_IP`, `IP_LOCKOUT_WINDOW_MINUTES`). `SECRET_KEY`
  still has an in-code default, but `docker-compose.yml`/`docker-compose.sqlite.yml` require
  `SECRET_KEY`/`POSTGRES_PASSWORD` to be set (`${VAR:?...}`) and fail fast with a pointer to
  `scripts/setup_env.py` instead of silently falling back to a repo-committed value — don't
  reintroduce a `:-default` fallback for either in the compose files.
- `core/database.py` — async engine/session setup and `Base`. `get_async_db` commits once at the end of
  a request if the handler didn't raise, and rolls back otherwise — repositories only ever `flush()`,
  they never commit, so don't add commits anywhere else. One deliberate carve-out: it also commits (rather
  than rolling back) on `AuthenticationError`, since login-failure bookkeeping (e.g. the failed-attempt
  counter in `AuthService.login`) is flushed right before that error is raised and must survive it. If you
  need writes to survive some other exception type, extend that one `except` clause — don't reach for a
  manual `session.commit()` in a service.
- `core/security.py` — PIN hashing (via `bcrypt` directly, **not** `passlib`: passlib's bcrypt backend
  self-test breaks under bcrypt ≥ 4.1, a live incompatibility, not a hypothetical) and JWT issue/decode.
- `core/email.py` — stub email "sender" (logs only) — see "Known gaps" above before assuming it sends.

**Dependency management is [uv](https://docs.astral.sh/uv/), not pip/venv.** `pyproject.toml` +
`uv.lock` (committed) are the source of truth; don't `pip install` anything directly or hand-edit
`.venv`. Runtime deps live in `[project.dependencies]`; dev-only tools (pytest, ruff, httpx) live in
`[dependency-groups].dev`, which `uv sync` installs by default (use `--no-dev` to skip, as the
Dockerfile does).

**Commands:**
```bash
cd backend
uv sync                                        # creates/updates .venv from uv.lock (incl. dev group)
uv run python scripts/setup_env.py sqlite      # or: postgres — generates .env non-interactively
uv run uvicorn app.main:app --reload --port 8000
uv run ruff check app/                         # lint — CI runs this, must be clean
uv run pytest -v                                # CI runs this
```
Swagger UI at `/docs`, health check at `/health`. Docker: `docker-compose up --build -d` (Postgres) or
`docker-compose -f docker-compose.sqlite.yml up --build -d` (SQLite) — the Dockerfile also uses uv
internally (multi-stage: installs the locked deps via `uv sync --frozen --no-dev`, then copies the
resulting `.venv` into the runtime image).

Added a new dependency? Run `uv add <package>` (or `uv add --group dev <package>` for dev-only tools)
instead of editing `pyproject.toml` by hand — it keeps `uv.lock` in sync automatically. If you do edit
`pyproject.toml` directly, run `uv lock` afterward and commit the updated `uv.lock`.

**Conventions:**
- All route handlers are `async def`; use the async session from `api/deps.get_db`, never a sync session.
- Integer autoincrement PKs (see `models/user.py`), not UUIDs — stay consistent with existing models.
- `created_at`/`updated_at` via `server_default=func.now()` / `onupdate=func.now()` on every table.
- New DB schema changes need an Alembic migration (`alembic/`) — don't rely on `Base.metadata.create_all`
  (nothing calls it; SQLite schema creation via `create_all` was deliberately removed so SQLite and
  Postgres go through the identical migration path). Alembic is the only schema authority for both
  SQLite and Postgres; run `alembic upgrade head` after pulling new migrations or setting up a fresh DB.
  If two branches each add a migration off the same parent, you'll get divergent heads on merge
  (`alembic heads` shows more than one) — resolve with `alembic merge heads` (see
  `448438686134_merge_heads.py` / `91bd6c67df47_merge_login_lockout_and_missing_indexes_.py` for
  precedent) rather than hand-editing `down_revision`.
- Run `uv run ruff check app/` before considering backend work done; CI will fail otherwise.

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

- `backend-ci.yml`: installs uv (`astral-sh/setup-uv`), `uv sync --frozen`, `uv run ruff check app/`,
  `uv run pytest -v` (against a fresh SQLite env via `setup_env.py sqlite`), then a Docker build. Keep
  backend changes lint-clean and test-covered.
- `pyproject.toml` pins `[tool.ruff.lint] select = ["E4", "E7", "E9", "F"]` explicitly. Newer ruff
  releases expand their implicit default rule set well beyond that (hundreds of extra rules, including
  a false-positive on every FastAPI `Depends(...)` default argument) — pinning keeps `ruff check`
  deterministic across ruff versions instead of silently growing scope on every dependency bump.
- `pyproject.toml` also sets `[tool.setuptools.packages.find] include = ["app*"]`. Without it, a flat
  local install (`uv sync`) fails with "Multiple top-level packages discovered" once both `app/` and
  `alembic/` exist side by side — this doesn't affect the Docker build (its builder stage runs
  `uv sync --no-install-project`, so it never builds the local package at all, only the third-party
  deps from the lockfile), but it would otherwise block local dev syncs and CI.
- `uv.lock` is committed and CI runs `uv sync --frozen` (fails instead of silently re-resolving if the
  lockfile is stale) — if you add/bump a dependency, run `uv lock` (or `uv add`/`uv add --group dev`,
  which updates the lock for you) and commit the result alongside the `pyproject.toml` change.
- `frontend-ci.yml`: validates Gradle build graph for Android, Web Wasm, and iOS framework compile targets
  on every PR touching `frontend/**`.

Only run/validate the pipeline(s) relevant to the code you changed.
