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

- Backend: v1 MVP REST surface is implemented — auth (email + 6-digit PIN chosen by the user at
  signup, JWT, forgot-PIN reissue), households (create/update, invites, join, member roles — Owner/Admin/Member, with
  single-holder ownership transfer, leave/remove), budgets,
  categories (with reassign-before-delete), transactions (role-scoped edit/delete, filterable by
  category/payer/type/payment mode/date range/amount range/merchant-or-category search), dashboard,
  and a polling activity feed. Follows a Router → Controller → Service → Repository layering under
  `app/` (`api/v1/endpoints/` → `controllers/` → `services/` → `repositories/`). `alembic/versions/`
  now holds a full migration chain (not a single initial migration) — schema has evolved
  incrementally (login lockout fields, per-IP login-failure table, denormalized/atomic household
  member counter, unique constraints, Numeric money columns, indexes, Owner-role backfill) since the original v1 cut; see
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
- Frontend: Phase 1 (core daily-use loop), most of Phase 3 (collaboration/profile), and Phase 2's
  onboarding/auth screens have landed (see "Frontend build plan" below) — feature-based navigation
  (Dashboard/Categories/History/Add Transaction/Profile/Household Members/Invite Member/Welcome/
  Auth/Backend Config/PIN Sent/Forgot PIN/Household Choice/Create Household/Join Household).
  Every repository is now real — `AuthRepository`, `CategoryRepository`, `TransactionRepository`,
  `DashboardRepository`, `ProfileRepository` (`RealAuthRepository`, `RealCategoryRepository`,
  `RealTransactionRepository`, `RealDashboardRepository`, `RealProfileRepository` — see "Real
  networking" under Phase 2 below and "Real networking: Category"/"Transaction"/"Dashboard"/
  "Profile" under Phase 1/3 below). The `Fake*Repository` classes are all still in the codebase as
  reference/offline-preview implementations (nothing in `AppContainer` constructs them anymore —
  see its class doc), and `fixtures/DummyScenario.kt` is correspondingly dead weight now rather
  than load-bearing; nothing currently reads it except those unused Fake classes and
  `App.kt`/`AppContainer`'s now-inert `scenario` parameter. `App.kt` gates on a persisted
  `AuthSession?`
  (`core/persistence/SettingsStorage`): `null` renders `OnboardingRoute`, non-null renders the
  Phase 1/3 main shell. The backend base URL is user-configurable via the Backend Configuration
  screen (PRD A0/Section 9.9: hosted by default, or a self-hosted custom URL), stored as a
  device-level `BackendConfig`, persisted the same way.

Don't assume a feature exists because it's in the PRD or in a model/schema — check the actual endpoint
router and frontend screens first.

### Known gaps (deliberately deferred, see PR discussion)

- **Email delivery is a stub.** `app/core/email.py` logs PIN and invite messages instead of sending
  them (no SMTP/SES integration yet). This only affects **forgot-PIN** (which still generates and
  "emails" a fresh PIN server-side — `AuthService.forgot_pin`) and invites: those PINs/tokens are
  **not** echoed back in any API response, so retrieving them requires reading server logs or
  querying the DB directly (see how `backend/tests/helpers.py` does it for tests, by monkeypatching
  the generators). **Signup is unaffected** — the user chooses and submits their own PIN
  (`UserCreate.pin`), so there's nothing to email or dig out of logs for that flow. Real delivery
  must land before forgot-PIN/invites are usable outside local dev.
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
- **No resend-invite endpoint on the backend**, and no frontend affordance for it either.
  `HouseholdService`/`InviteRepository` only have create/list/revoke — no resend. The frontend
  originally added a matching `ProfileRepository.resendInvite` (fake-repo-only, no real endpoint to
  call), but it was removed since there was nothing real for it to do; see Phase 3 below. If resend
  is wanted later, add the backend endpoint first (likely: reissue token + `expires_at`, re-trigger
  `send_invite_email`), then bring the frontend button back against real networking.

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
  access-control deps (`get_household_membership`, `require_admin_membership` — passes Admin *or*
  Owner, since Owner is a superset of Admin permissions, `get_current_household`). Owner-only actions
  (transferring ownership) enforce that narrower check themselves in the service layer, on top of
  `require_admin_membership`.
- `controllers/` — thin orchestration between routers and services; maps request schemas to service
  calls and service results back to response schemas. No query-building here — a few controllers
  (`auth_controller.py`, `dashboard_controller.py`, `user_controller.py`) import `AsyncSession`
  purely to type-hint the `db` param they pass through to services, but none construct queries
  with it directly.
- `services/` — business rules (role permissions, the 3-member cap, future-date rejection, cycle
  math in `cycle_utils.py`, delete-blocked-by-transactions, invite expiry, "always exactly one
  Owner" (single-holder, transferred via `HouseholdService._transfer_ownership` — only the current
  Owner can promote an Admin to Owner, which auto-demotes the outgoing Owner to Admin; the Owner
  can't be removed/demoted/leave directly), login lockout/rate-limit — see "Auth hardening" above).
  Raises the domain exceptions in
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
  `generate_pin()` is only used by `AuthService.forgot_pin` now — signup takes the user's own PIN
  (`UserCreate.pin`, validated `^\d{6}$` in `schemas/user.py`) and just hashes it, it doesn't generate
  one. Don't reintroduce server-generated PINs at signup without a product reason; see PRD Section 9.1.
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
Swagger UI at `/docs`, health check at `/health` (DB-backed), and a DB-independent `/ping` liveness
check (`api/v1/endpoints/health.py`) for the frontend's Backend Configuration "Server Reachable"
validation — `/health` isn't suitable there since it depends on the target server's DB being
configured/online, which a not-yet-validated custom URL may not guarantee. Docker: `docker-compose up --build -d` (Postgres) or
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

## Frontend build plan

The full PRD frontend surface (~18 screens: onboarding/auth, dashboard, categories, transactions,
collaboration, profile) is being built in three phases, backed by dummy/fake repository data until
real Ktor networking is wired up (see "Frontend dummy data scenarios" below). Update this section's
checkboxes as phases land so the plan survives across sessions.

- [x] **Phase 1 — Core daily-use loop (PRD B/C/D).** Dashboard (empty states, FAB long-press
  Add Expense/Income shortcuts, tap-through), Category Limits (C1, admin-gated, including the
  Add Category form — name/limit/icon-grid picker, `feature/category/presentation/AddCategory*`)
  + Category Detail (C3), Transaction History (D2, grouped/search/filter) + Transaction Detail
  (D3, role-gated edit/delete) + Add Transaction (D1). Persistent bottom nav (Dashboard,
  Categories, Add [FAB], History, Profile) with a minimal read-only Profile stub — full profile
  editing is Phase 3. Feature-based folders under
  `composeApp/src/commonMain/kotlin/com/famex/feature/{dashboard,category,transaction,
  profile}/{data,domain,presentation}`, shared bits in `core/` (`navigation`, `model`, `ui`, `util`,
  `di`), dummy scenarios in `fixtures/`. Category deletion (C1) is implemented: a "more_vert"
  admin menu on Category Detail's summary card (`CategoryAdminMenu` in `CategoryDetailScreen.kt`,
  from Stitch's "Category Detail (Admin Menu)" screen) opens a "Delete Category" action, which
  shows `core/ui/DeleteCategoryDialog.kt` (Stitch's "Delete Category Confirmation" screen) — if the
  category has transactions it requires picking a reassign target before enabling delete (blocking
  otherwise, per PRD C1), matching the backend's `DELETE .../categories/{id}?reassign_to_category_id=`
  contract. `TransactionRepository.reassignCategory` and `CategoryRepository.deleteCategory` back
  this — both are real now (see below), so reassign-then-delete round-trips the actual backend.
  The mockup's Edit Category/Category Settings menu items were left out — no corresponding
  feature exists yet, so they'd be dead entries.
  **Real networking: `CategoryRepository` is now real** (`RealCategoryRepository`,
  `feature/category/data/`) — same `AuthApiService`/DTO/mapper shape as `RealAuthRepository`
  (`CategoryApiService` + `feature/category/data/remote/dto/`, `data/mapper/`), hitting
  `/households/{id}/categories`. `CategoryRepository`'s interface takes no household id (it's
  called from `category`, `transaction`, and `dashboard` presentation code written before real
  networking existed) — `RealCategoryRepository` instead resolves it from
  `core/network/HouseholdRequestContext.kt` (`HouseholdRequestContextProvider`), which bundles the
  current access token +
  `BackendConfig` + household id (the last of those from `core/session/CurrentHouseholdHolder`, a
  small mutable holder `App.kt` keeps in sync with the signed-in session's `household.id` — set on
  restore/onboarding-complete, cleared on sign out). `RealTransactionRepository` uses the same
  provider — any future `Real*Repository` with the same "which household" problem (Dashboard)
  should too, rather than inventing another mechanism. One decoding gotcha worth knowing about:
  the backend's `monthly_limit`/`spent` are Pydantic `Decimal` fields, which serialize
  as JSON **strings** (`"12.50"`, confirmed against Pydantic v2's default encoder), not numbers —
  `feature/category/data/remote/dto/CategoryDtos.kt` declares `monthlyLimit: String` and
  `core/network/dto/CategoryDto.kt` (`CategoryWithStatsDto`) declares `spent: String`, with
  `CategoryMappers.kt` doing the `.toDouble()` conversion for both; declaring them `Double`
  directly would fail to decode. `remaining` is never decoded at all — it's derived client-side
  from `monthlyLimit`/`spent` and intentionally omitted from the DTO. Same string-decoding gotcha
  applies to `Transaction.amount` (see below). `updateCategoryLimits` does one `PATCH` per changed category —
  no batch-update endpoint exists, same constraint `AuthRepository.setupCategories` already
  documented for create. `BackendConfig` persistence moved out of `RealAuthRepository` into a
  shared `core/network/BackendConfigStorage`, since every `Real*Repository` needs it, not just auth.
  **Real networking: `TransactionRepository` is now real too** (`RealTransactionRepository`,
  `feature/transaction/data/`, same `remote`/`dto`/`mapper` shape). `GET
  /households/{id}/transactions` is paginated (`Page[TransactionResponse]`, capped at 200/page
  server-side via `TransactionFilterParams.limit`), but `TransactionRepository.getTransactions()`
  has no pagination in its signature — History's filtering/grouping is all client-side over the
  full list (`HistoryController.load`) — so `TransactionApiService.listAllTransactions` pages
  through with `limit=200` until it's collected everything (bounded at 25 pages as a safety net,
  not because that's an expected ceiling). `reassignCategory` has no bulk-move endpoint either:
  it lists every transaction in the source category (same pagination helper, filtered by
  `category_id`) and issues one `PATCH .../transactions/{id}` per transaction with just
  `category_id` changed. `TransactionResponse.amount` is the same Decimal-serializes-as-string
  gotcha as Category's fields. `paid_by_id` is always sent explicitly on create/update (not
  omitted to let the backend default it to the caller) since Add/EditTransactionController let
  the user pick *any* household member as payer, not just themselves. `UserResponseDto` +
  `DisplayModeDto` moved out of `feature/auth/data/remote/dto/` into shared
  `core/network/dto/`+`core/network/mapper/` once Transaction needed the same nested-user shape
  for `paid_by_user` — reuse those rather than re-declaring a per-feature copy.
  **Real networking: `DashboardRepository` is now real too** (`RealDashboardRepository`,
  `feature/dashboard/data/`). `DashboardData` needs three things no single backend response
  provides together: `GET /households/{id}/dashboard` (budget + category snapshots, no
  household), `GET /households/{id}` (the full household — same call `AuthApiService.getHousehold`
  already makes, reusing the shared `HouseholdResponseDto`), and `GET
  /households/{id}/activity-feed` (paginated; fetched with `limit=5` to match the fixture preview
  size `dummyDashboardActivityFeed` used, since this is a preview, not full history). This is what
  finally pushed `HouseholdResponseDto`/`HouseholdMemberResponseDto`/`MemberRoleDto` and
  `CategoryWithStatsDto`/`TransactionTypeDto` out of `feature/auth`/`feature/category`/
  `feature/transaction` into shared `core/network/dto/`+`core/network/mapper/` too (same rationale
  as `UserResponseDto` above — Dashboard is now a second/third consumer of each). **The activity
  feed previously had two data gaps (created_by_user vs paid_by_user, and no category_id) — both
  are now fixed: `ActivityFeedItem` returns `paid_by_user` + `category_id` alongside
  `created_by_user`; see `backend/app/schemas/dashboard.py` and `_to_activity_item` in
  `dashboard_controller.py`.
- [x] **Phase 2 — Onboarding & auth funnel (PRD A) — screens are wired to real backend
  networking.** (Note: this bullet originally read "screens landed on fake repos, no real
  networking yet" — that was true when Phase 2 was first built on 2026-08-04, but stale after
  commit `c639b37` wired `RealAuthRepository` into `AppContainer` on 2026-08-05; see "Real
  networking" further down in this bullet for what's actually real.) Covers A0 (backend endpoint
  selection), A1 (welcome), A2/A2a (signup, PIN
  verify via login, forgot PIN), and household create/join — all in `feature/auth/
  {data,domain,presentation}`, its own `core/navigation/OnboardingScreen.kt` +
  `OnboardingNavController.kt` (mirrors `AppNavController` — separate back stack, no bottom nav,
  torn down once a household is ready). `App.kt` now gates on `AuthSession?`: `null` renders
  `OnboardingRoute`, non-null renders the existing Phase 1/3 main shell (`MainAppShell`). Screens:
  Welcome → Auth (Sign In/Sign Up as tabs on one screen, matching the Stitch pair) → Backend
  Configuration (gear icon on Auth) → Forgot PIN → PIN Sent → Household Choice → Create
  Household → Budget Goal → Configure Categories, or Join Household (Join skips Budget
  Goal/Configure Categories entirely — it attaches to a household someone else already
  budgeted/categorized). `FakeAuthRepository` seeds one demo account (`alex@example.com`,
  PIN `123456`) whose household matches the rest of the app's `DummyScenario` fixtures — sign in
  as that account to preview the full authenticated app; a fresh sign-up gets its own isolated
  in-memory household that only the onboarding screens see, since it's deliberately **not** wired
  into the other `Fake*Repository` instances (see the class doc on `FakeAuthRepository`).
  The system back button is now wired into both hand-rolled nav stacks via a cross-platform
  `BackHandler` (expect/actual — real handler on Android, no-op on iOS, which has no hardware
  back button), disabled at each stack's root so back there still falls through to the platform
  default (previously, system back bypassed `OnboardingNavController`/`AppNavController` entirely
  and could finish the Activity outright, e.g. exiting the app from the Auth screen).
  **Sign Up takes a user-chosen PIN** (Create PIN + Confirm PIN, validated 6-digit + matching in
  `AuthController.onSignUp`) matching the backend's `UserCreate.pin` contract — the PIN is no
  longer server-generated/emailed at signup, so `AuthController.onSignUp` logs the user straight
  in afterward (`AuthEvent.LoggedIn`) instead of routing through a "check your email" step; **PIN
  Sent is now forgot-PIN only** (`OnboardingScreen.PinSent` dropped its `PinSentContext` param —
there's only one context left). The "Server Reachable" live-validation UI on Backend
   Configuration is now implemented — `BackendConfigController.schedulePing` debounces URL
   input (500ms) and calls `AuthRepository.checkServerReachable` which hits the target server's
   DB-independent `GET /api/v1/ping` endpoint (see `backend/app/api/v1/endpoints/health.py`).
   The `ReachabilityIndicator` composable renders all five states (IDLE, INVALID, CHECKING,
   REACHABLE, UNREACHABLE) and the Save button is disabled until the custom URL is confirmed
   REACHABLE (`canSave` in `BackendConfigUiState`).
  **Budget monthly-goal-amount + initial category configuration (A3/A4) now land too:** after
  Create Household succeeds, Budget Goal (`BudgetGoal*` in `feature/auth/presentation`) collects
  a budget name/period/monthly goal amount (or can be skipped, completing onboarding without a
  budget), then Configure Categories (`ConfigureCategories*`) offers the 6 starter categories
  from the Stitch mockup as a checkbox list with per-category monthly limits, an "Automated
  Distribution" toggle that splits the goal amount evenly across whichever are checked, and an
  "Add custom category" row. Both call new `AuthRepository.setupBudget`/`setupCategories` methods
  (mirroring `createHousehold`) — `FakeAuthRepository`'s implementations just validate the
  household exists, matching this phase's fake-repo/no-real-networking stance; there's no
  dedicated backend "onboarding batch" endpoint, so the real implementation will need one
  `POST .../budgets` call + one `POST .../categories` call per category, same as `feature/
  category`'s Add Category flow does today. `AuthSession` and `BackendConfig` now survive a cold
  start via `core/persistence/SettingsStorage` — a hand-rolled key-value store (`expect`/`actual`:
  `SharedPreferences` on Android, `NSUserDefaults` on iOS), deliberately not DataStore/Room, same
  version-risk reasoning as avoiding Koin/navigation-compose (see "Architecture choices" below).
  `AuthRepository.getPersistedSession`/`persistSession`/`clearPersistedSession` (and
  `getBackendConfig`/`setBackendConfig`, now backed by the same storage instead of an in-memory
  var) serialize `AuthSession`/`BackendConfig` to JSON via `kotlinx.serialization` — both models
  (and `Household`/`HouseholdMember`/`PendingInvite`/`User`) are now `@Serializable`;
  `Household.joinCodeExpiresAt` uses kotlinx-datetime's built-in `LocalDateIso8601Serializer`.
  `App.kt` gates its first frame on a `getPersistedSession()` read (blank `Surface` while
  `isRestoringSession`) so a signed-in cold start renders straight into `MainAppShell` instead of
  flashing `OnboardingRoute` first; `onOnboardingComplete` persists, Sign Out clears. A Sign Out
  row + `SignOutDialog` confirmation live on the Profile screen (mirrors `DeleteCategoryDialog`'s
  confirm-destructive-action pattern; uses `Icons.AutoMirrored.Filled.Logout`, not the deprecated
  non-mirrored `Icons.Default.Logout`, to avoid RTL flip issues) — confirming clears both the
  in-memory `AuthSession` and the persisted one, dropping `App.kt` back to `OnboardingRoute` for
  good (not just until the next cold start).
  **`AuthRepository` is now real** (`RealAuthRepository` in `feature/auth/data/`, replacing
  `FakeAuthRepository` in `AppContainer` — the fake implementation is left in place as a
  reference/offline fallback but nothing wires to it anymore). Backed by `core/network/`: a
  shared `HttpClient` (`HttpClientFactory.kt`), a typed `AppException` hierarchy + `safeApiCall`
  wrapper that parses both of the backend's error body shapes (`{"detail": "msg"}` from the
  `AppError` hierarchy and `{"detail": [{"msg": ...}]}` from raw pydantic validation errors),
  `AuthTokenStorage` (access-token-only — the backend issues no refresh token, so an expired
  token means signing in again, not a silent refresh), and `BackendConfig.apiUrl(path)` to build
  request URLs. `AuthApiService` + `feature/auth/data/remote/dto/`/`data/mapper/` hold the
  request/response DTOs and DTO→domain mapping. `setupBudget`/`setupCategories` do exactly the
  `POST .../budgets` + one `POST .../categories`-per-category sequence anticipated above — there's
  still no batch "onboarding" endpoint. Two small backend additions were needed to make this work
  at all: `HouseholdMemberResponse` gained a `household_id` field (was missing it entirely, so a
  client couldn't tell which household `POST /households/join` had just added it to), and a new
  `GET /users/me/household` endpoint (nullable `HouseholdResponse`) resolves the current user's
  household — neither `/auth/login` nor `/households/join` returned one, and v1 caps a user to a
  single household anyway, so this is how `RealAuthRepository.login`/`joinHousehold` now resolve
  it. `Household.joinCodeExpiresAt` is populated as `created_at + 7 days` (`INVITE_EXPIRY_DAYS`)
  since the backend still has no real household-level join code (see the E1 gap under Phase 3) —
  a placeholder, not real data.
- [~] **Phase 3 — Collaboration & full profile (PRD E1/E2) — mostly landed, one gap remains.**
  Done: household member list with roles (`feature/profile/presentation/HouseholdMembers*`),
  promote/demote/remove for any role (`ac07262`, `1837cfe`), invite-by-email now creates a revocable
  pending invite instead of adding the member directly (`core/model/Household.PendingInvite` +
  `ProfileRepository.inviteMember`/`revokeInvite`), rendered as
  `PendingInviteCard` on `HouseholdMembersScreen.kt` with a Revoke action and a teal-tinted Invite
  CTA row (Stitch "Member Management (With Invite CTA)" / "(With Pending Invite)"), with
  `InviteMemberScreen.kt` (Stitch "Invite Options") slimmed down to just the email-invite and
  join-code cards — the duplicate current-members list that used to live there was removed since
  pending invites now show on `HouseholdMembersScreen` instead. A Resend Invite action was tried and
  then deliberately dropped: there's no backend resend endpoint (see "Known gaps" above), so it had
  nothing real to call once networking lands — don't re-add it without the backend endpoint first.
  Editable profile (name/nickname, read-only email), household currency/language, display mode
  preference — all in `feature/profile/`. **Role-gating is now implemented** (`c45d365`, `8d111ed`,
  `a60d6c1`): `ProfileScreen` derives `currentUserRole` (via `Household.currentMemberRole`,
  `core/model/Household.kt`) and hides the whole "Household Settings" card (Manage Members,
  currency, language) from plain Members — `canManageHousehold = currentUserRole?.isAdminOrOwner`
  — instead of only 403ing after the fact; Personal Settings (display mode) stays visible to
  everyone. `HouseholdMembersScreen` similarly hides each row's admin action menu
  (`canManage`) for plain-Member viewers, hides a member's own row's self-action menu, and only
  offers "Promote to Owner" when the viewer *is* the current Owner (Admins can't see it on other
  Admins). `CurrentHouseholdHolder` and `App.kt` were extended to carry the signed-in user's id
  so role can be derived client-side without an extra call.
  **Gap:** the 7-day join-code expiry is now modeled and shown (`Household.joinCodeExpiresAt`,
  mirroring the backend's `Invite.expires_at`/`INVITE_EXPIRY_DAYS`, rendered on
  `InviteMemberScreen.kt` in place of the old static "7 days" copy), but shareable join link/QR
  code (the other half of E1) is still not implemented.
  **Real networking: `ProfileRepository` is now real** (`RealProfileRepository`,
  `feature/profile/data/`, same `remote`/`dto`/`mapper` shape as the others) — this was the last
  fake repository; every repository in the app is now real (see the top of this file). Fans out
  across `/users/me` (get/update) and `/households/{id}` + `/households/{id}/invites` (get/update
  household, list/create/revoke invites) + `/households/{id}/members/{id}` (role update, remove).
  `HouseholdResponse` never includes pending invites (they're a separate admin-only list) — every
  method that returns a `Household` goes through a private `fetchHousehold()` that fetches both
  and stitches them together, catching a `PermissionDeniedException` on the invites call
  specifically (since a plain Member 403s there, per the gating gap above) and treating it as "no
  invites to show" rather than failing the whole screen load.
  **Push notifications toggle removed from the UI (not the repository).** `updatePushNotifications`
  has no backend field to persist to (`UserResponse`/`User` model have none) — it was only ever
  applied locally on top of a fresh `/users/me` fetch and never actually saved, silently resetting
  to the default (`true`) on every reload. Since that's misleading (a toggle that visually "stays
  on" but never really persists anything), the Push Notifications row was removed from
  `ProfileScreen.kt` (`Personal Settings` card) along with the `onPushNotificationsToggle` plumbing
  through `ProfileRoute.kt` and `ProfileController.kt` (the latter's handler function was deleted
  outright, not just disconnected — nothing called it anymore). `ProfileRepository.
  updatePushNotifications` and both implementations (`RealProfileRepository`,
  `FakeProfileRepository`) were deliberately left in place — this is a UI-visibility fix, not a
  capability removal, so there's a working repository method ready to wire back in once the
  backend actually has a `User.push_notifications_enabled`-style field (`UserUpdate`/`UserResponse`
  in `backend/app/schemas/user.py`, plus a migration) to persist to. Re-adding the row is then a
  pure UI change: restore the `Switch` block in `ProfileScreen.kt` and the two lines of plumbing in
  `ProfileRoute.kt`/`ProfileController.kt` — see git history for the exact removed block if needed.

**Offline support (PRD §7) — queued transaction writes + read-through cache.** Every feature
repository in `AppContainer` is now an `OfflineFirst*Repository` wrapping its `Real*Repository`.

- **Reads** are network-first with cache fallback: `core/offline/NetworkFirstRead.kt` catches
  `AppException.NetworkException`/`TimeoutException` and serves the last successful fetch from
  `core/cache/LocalCacheStore` (JSON blobs in `core/cache/LocalFileStorage`, an expect/actual
  file store — `filesDir` on Android, `Library/Caches` on iOS; deliberately not DataStore/Room,
  same version-risk reasoning as `SettingsStorage`). Any other error (auth, permission, validation)
  propagates untouched so a stale cache cannot mask a real rejection.

- **Transaction writes** are the only offline write surface (per PRD §7: "Transactions can be added
  offline and sync automatically on reconnect"): `OfflineFirstTransactionRepository`'s
  `addTransaction` / `updateTransaction` / `deleteTransaction` park the operation in
  `core/offline/OfflineQueue` (persistent FIFO JSON array via `SyncManager.enqueue`) when the
  network call fails, and return a synthetic `Transaction` with a negative temp id + `clientId`
  (`Transaction.isPending`) so the UI shows it immediately. Deleting a still-pending create drops
  the queued `AddTransaction` outright. Every other write surface (categories, profile, members)
  deliberately passes through and surfaces the `NetworkException` inline.

- **Sync:** `core/offline/SyncManager` drains the queue FIFO against the real transaction repo
  (never the wrapper) on each offline→online transition — `App.kt` observes
  `core/util/ConnectivityObserver` (`ConnectivityManager` on Android, Network.framework's
  `nw_path_monitor_*` C API on iOS — K/N 1.9.23 exposes the C API, not the ObjC NWPathMonitor
  class). `ConflictResolver` implements "server wins, append-only safe": adds never conflict (new
  rows); edit/delete conflicts and permanent 4xx rejections discard the change + emit a
  `SyncEvent.Rejected` (snackbar); transient / 5xx / 429 / expired-token keep it queued. Pending
  creates resolve to server ids via a clientId→serverId map populated as adds replay (queue is
  FIFO, so the Add always precedes ops that reference it). `pendingCount` StateFlow drives the
  amber "N pending" top-bar badge. Cache is updated on every successful sync so offline reads
  stay current. Unit tests live in `composeApp/src/commonTest/.../core/offline/`.

**Architecture choices made in Phase 1 (carry forward into later phases):**
- Navigation is a hand-rolled `core/navigation/AppNavController` (sealed `Screen` + back-stack list),
  not `androidx.navigation.compose` — avoids version risk against the pinned Kotlin 1.9.23/Compose
  Multiplatform 1.6.1 toolchain. Keep using it rather than introducing a nav library mid-build.
- DI is a manual `core/di/AppContainer` (composition root + `CompositionLocal`), not Koin — repos are
  interface-first (`XRepository` + `FakeXRepository`) so swapping in Koin + real Ktor implementations
  later only touches the container, not screens.
- State holders are plain Kotlin classes exposing `StateFlow<UiState>`/`SharedFlow<Event>` with a
  manually-scoped `CoroutineScope`, not `androidx.lifecycle.ViewModel` (same version-risk reasoning).

**Frontend dummy data scenarios** (`fixtures/DummyScenario.kt`) — code-level switch only (change the
constant in `App.kt` and rebuild; no in-app dev switcher by design): `NoBudgetSetup`,
`EmptyBudgetNoTransactions`, `HealthyMidMonth`, `NearLimitAmber`, `OverBudgetCoral`, `SoloBudgeter`,
`FullHouseholdThreeMembers`, `LongTransactionHistory`, `SimulatedLoadingAndError`. Fake repos add a
short `delay()` before returning so Loading states are real, and `SimulatedLoadingAndError` forces a
throw once so Error/retry UI is exercised too — these aren't just Success-state fixtures.

## Key business rules to respect (from the PRD)

These affect any backend logic or UI you write around budgets/transactions — get them from the PRD, not
assumptions:
- Roles are **Owner / Admin / Member**. Members can add/edit/delete only their *own* transactions;
  Admins and the Owner can edit/delete anyone's, and manage categories, limits, invites, and household
  currency/language. Owner is a **single-holder role per household** — exactly one member holds it at
  all times, transferred (not duplicated): only the current Owner can promote an existing Admin to
  Owner, which auto-demotes the outgoing Owner to Admin in the same operation. The Owner can't be
  removed, demoted, or leave the household directly — ownership must be transferred to an Admin first.
- One budget per household, one currency per household (not per-transaction).
- Category limits **reset every cycle with no rollover** — but historical transactions/snapshots for prior
  cycles must remain intact and queryable by date range.
- Household hard cap: **3 members** (including the Owner) in v1.
- Future-dated transactions are **disallowed**.
- Auth is email + 6-digit PIN, not password-based. The PIN is **user-chosen at signup**
  (`UserCreate.pin`) — the backend only generates/emails a PIN for the forgot-PIN recovery flow,
  not at signup.
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
