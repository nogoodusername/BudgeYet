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
  Auth/Backend Config/PIN Sent/Forgot PIN/Household Choice/Create Household/Join Household) backed
  by fake repositories and dummy data scenarios, no real networking yet. `App.kt` now gates on an
  in-memory `AuthSession?`: `null` renders `OnboardingRoute`, non-null renders the Phase 1/3 main
  shell — but the session itself doesn't persist (no DataStore yet), so every cold start begins
  signed out again. The backend base URL is user-configurable via the Backend Configuration screen
  (PRD A0/Section 9.9: hosted by default, or a self-hosted custom URL), stored as a device-level
  `BackendConfig` — but only in-memory for now, same persistence gap as the session.

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
  calls and service results back to response schemas. No SQLAlchemy imports here.
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
  this on the fake-repo side. The mockup's Edit Category/Category Settings menu items were left out
  — no corresponding feature exists yet, so they'd be dead entries.
- [~] **Phase 2 — Onboarding & auth funnel (PRD A) — screens landed on fake repos, no real
  networking yet.** Covers A0 (backend endpoint selection), A1 (welcome), A2/A2a (signup, PIN
  verify via login, forgot PIN), and household create/join — all in `feature/auth/
  {data,domain,presentation}`, its own `core/navigation/OnboardingScreen.kt` +
  `OnboardingNavController.kt` (mirrors `AppNavController` — separate back stack, no bottom nav,
  torn down once a household is ready). `App.kt` now gates on `AuthSession?`: `null` renders
  `OnboardingRoute`, non-null renders the existing Phase 1/3 main shell (`MainAppShell`). Screens:
  Welcome → Auth (Sign In/Sign Up as tabs on one screen, matching the Stitch pair) → Backend
  Configuration (gear icon on Auth) → PIN Sent (reused for both post-signup and forgot-PIN,
  differing only in copy — see `PinSentContext`) → Forgot PIN → Household Choice → Create
  Household / Join Household. `FakeAuthRepository` seeds one demo account (`alex@example.com`,
  PIN `123456`) whose household matches the rest of the app's `DummyScenario` fixtures — sign in
  as that account to preview the full authenticated app; a fresh sign-up gets its own isolated
  in-memory household that only the onboarding screens see, since it's deliberately **not** wired
  into the other `Fake*Repository` instances (see the class doc on `FakeAuthRepository`).
  **Known frontend/backend mismatch:** the Sign Up screen has no Create PIN field, but the
  backend now requires a user-chosen `UserCreate.pin` at signup (`feature/create-pin-signup`,
  merged to main) rather than generating and emailing one — the backend changed its mind on this
  after this branch's screens were built. Harmless today since everything here still runs on
  `FakeAuthRepository` dummy data with no real networking, but don't wire real Ktor networking to
  `AuthRepository.signUp` without adding the PIN field first; see `feature/create-pin-signup-frontend`
  for that fix. The "Server Reachable" live-validation UI on Backend Configuration also isn't
  implemented — there's no real request to validate a custom URL against yet.
  **Not covered by this batch:** budget monthly-goal-amount + initial category configuration
  (A3/A4) — Create Household only covers name/currency/cycle start day. `AuthSession` and
  `BackendConfig` are in-memory only (no DataStore/local persistence layer exists yet), so both
  reset on cold start — the app cannot actually stay signed in across restarts until that lands.
  There's also no sign-out affordance anywhere in the main shell yet. This phase still carries the
  real Ktor networking work (see below); everything today runs on `FakeAuthRepository` dummy data.
- [~] **Phase 3 — Collaboration & full profile (PRD E1/E2) — mostly landed, one gap remains.**
  Done: household member list with roles (`feature/profile/presentation/HouseholdMembers*`),
  promote/demote/remove for any role (`ac07262`, `1837cfe`), invite-by-email now creates a revocable
  pending invite instead of adding the member directly (`core/model/Household.PendingInvite` +
  `ProfileRepository.inviteMember`/`revokeInvite`, backed by `FakeProfileRepository`), rendered as
  `PendingInviteCard` on `HouseholdMembersScreen.kt` with a Revoke action and a teal-tinted Invite
  CTA row (Stitch "Member Management (With Invite CTA)" / "(With Pending Invite)"), with
  `InviteMemberScreen.kt` (Stitch "Invite Options") slimmed down to just the email-invite and
  join-code cards — the duplicate current-members list that used to live there was removed since
  pending invites now show on `HouseholdMembersScreen` instead. A Resend Invite action was tried and
  then deliberately dropped: there's no backend resend endpoint (see "Known gaps" above), so it had
  nothing real to call once networking lands — don't re-add it without the backend endpoint first.
  Editable profile (name/nickname, read-only email), household currency/language (admin-gated),
  display mode preference — all in `feature/profile/`.
  **Gap:** the 7-day invite expiry isn't modeled or shown anywhere in the UI (revoke works, expiry
  doesn't), and shareable join link/QR code (the other half of E1) is still not implemented. Since
  this all still runs on fake repos, wiring real invite semantics (backend `Invite.token`/
  `expires_at`) likely lands together with Phase 2's networking work rather than as a separate
  frontend-only fix.

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
