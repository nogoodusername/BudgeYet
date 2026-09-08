# Getting Started

Local development setup for the two independently‑built projects in this monorepo:
`backend/` (FastAPI) and `frontend/` (Compose Multiplatform). They have separate
toolchains and separate CI — only set up the one you're working on.

For self‑hosting a real deployment instead of a dev setup, see [Deployment](deployment.md).
For running the mobile apps on devices/emulators, see [Running on Mobile](running-on-mobile.md).

---

## Backend (`backend/`)

**Stack:** FastAPI · Pydantic v2 · SQLAlchemy 2.0 async · Alembic · SQLite or PostgreSQL.
Dependencies and the virtualenv are managed by [`uv`](https://docs.astral.sh/uv/)
(`uv.lock` is committed — don't hand‑edit `.venv`).

### 1. Install `uv`

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

`uv` reads `.python-version` (3.11) and fetches that interpreter automatically if
it's missing. Every `uv run …` syncs `.venv` from `uv.lock` first, so there is no
separate install step.

### 2. Configure the environment and database

```bash
cd backend

uv run python scripts/setup_env.py          # interactive: choose SQLite or Postgres
# or non-interactive:
uv run python scripts/setup_env.py sqlite
uv run python scripts/setup_env.py postgres
```

This generates `backend/.env`. Database choice is controlled by `DATABASE_TYPE`
(`sqlite` | `postgres`); code stays driver‑agnostic so either works.

### 3. Apply migrations

Alembic is the single source of schema truth for **both** databases — the app
never auto‑creates tables.

```bash
uv run alembic upgrade head
```

Run this again whenever you pull new migrations. If a merge produces divergent
heads (`alembic heads` shows more than one), resolve with `alembic merge heads`.

### 4. Run the server

```bash
uv run uvicorn app.main:app --reload --port 8000
```

- Swagger UI: <http://localhost:8000/docs>
- ReDoc: <http://localhost:8000/redoc>
- Health: <http://localhost:8000/health>

Prefer an activated shell? `source .venv/bin/activate` works — `uv sync` manages
that same `.venv`.

### Run with Docker instead

```bash
# PostgreSQL stack
docker compose up --build -d
# SQLite stack
docker compose -f docker-compose.sqlite.yml up --build -d
# then, inside the container:
docker compose exec backend alembic upgrade head
```

### Tests

```bash
uv run pytest -v
```

Integration tests spin up an in‑memory SQLite DB per test and drive the API
through `httpx.AsyncClient` — no `.env` or local DB required. Run
`uv run ruff check app/` before opening a PR; CI fails otherwise.

### Email in development

Outbound email (forgot‑PIN, invites) goes through Resend and is gated by
`RESEND_API_KEY`. Left blank (the default), the backend **logs** the PIN/invite
instead of sending it. Signup is unaffected — the user chooses their own PIN.

---

## Frontend (`frontend/`)

**Stack:** Kotlin Multiplatform + Compose Multiplatform (Android, iOS, Web),
Ktor client. Package root `com.budgeyet`. Toolchain is pinned — use the Gradle
wrapper (`./gradlew`), JDK 17.

The web target is **Kotlin/JS** (`js(IR)`) rendering Compose to an HTML canvas —
not `wasmJs` (Ktor 2.3.9 has no Wasm engine). Output is a static webpack bundle.

### Web

```bash
cd frontend
./gradlew :composeApp:jsBrowserDevelopmentRun     # serves http://localhost:8080, hot-reload
```

### Android

```bash
./gradlew :composeApp:assembleDebug               # build the debug APK
./gradlew :composeApp:installDebug                # build + install on the attached device/emulator
```

Or open `frontend/` in Android Studio and run the `composeApp` configuration.

### iOS

```bash
open frontend/iosApp/iosApp.xcodeproj
```

Select a simulator (or a signed device) and press Run — Xcode triggers the Gradle
step that compiles the Kotlin framework. Command‑line and SideStore flows are in
[Running on Mobile](running-on-mobile.md).

### Point the app at your backend

By default clients talk to the hosted API. To use your local server, open the
app's **Backend Configuration** screen (gear icon on the auth screen), choose
**Custom URL**, and enter `http://localhost:8000` (or your machine's LAN address
for a physical device).

### Tests

```bash
./gradlew :composeApp:iosSimulatorArm64Test       # runs commonTest via the iOS simulator target
```
