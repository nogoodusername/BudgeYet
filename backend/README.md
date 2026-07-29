# fam-ex Backend Service

FastAPI-powered REST API backend for the `fam-ex` collaborative household budget application.

---

## 🛠️ Tech Stack & Key Features
- **FastAPI**: Modern, high-performance web framework for Python 3.11+.
- **OpenAPI 3.0**: Auto-generated API documentation accessible at `/docs`.
- **SQLAlchemy 2.0 Async ORM**: Asynchronous data access layer.
- **Database Driver Choice**: Configurable at setup time for **Local SQLite (`aiosqlite`)** or **PostgreSQL (`asyncpg`)** (Supabase / Aiven / Local Postgres).
- **Alembic**: Database migration management.
- **[uv](https://docs.astral.sh/uv/)**: Dependency & virtualenv management (`uv.lock` is committed — don't hand-edit `.venv`).
- **Docker**: Containerized deployment for local development and production.

---

## 📦 Installing uv

If you don't have it yet:

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

See the [uv install docs](https://docs.astral.sh/uv/getting-started/installation/) for other platforms.
uv reads `.python-version` (3.11) and will fetch that interpreter automatically if it's missing locally.

---

## ⚙️ Initial Project Setup & Database Driver Choice

Run the setup helper script to configure your environment and database engine choice:

```bash
# Interactive setup
uv run python scripts/setup_env.py

# Non-interactive SQLite setup
uv run python scripts/setup_env.py sqlite

# Non-interactive Postgres setup
uv run python scripts/setup_env.py postgres
```

This generates `.env` with the chosen database configuration. `uv run` transparently syncs `.venv` from
`uv.lock` first if it's out of date, so there's no separate "install" step to remember.

---

## 🧬 Applying Database Migrations

Alembic is the single source of schema truth for both SQLite and Postgres — the app never
auto-creates tables. After setup (or whenever new migrations land), apply them:

```bash
uv run alembic upgrade head
```

---

## 🐳 Running with Docker

### PostgreSQL Setup (Default)
```bash
docker-compose up --build -d
```

### SQLite Setup
```bash
docker-compose -f docker-compose.sqlite.yml up --build -d
```

Either way, apply migrations inside the running container before using the API:
```bash
docker-compose exec backend alembic upgrade head
```

---

## 💻 Running Locally without Docker

```bash
# Creates/updates .venv from pyproject.toml + uv.lock (installs the dev group too)
uv sync

# Run FastAPI server
uv run uvicorn app.main:app --reload --port 8000
```

Prefer an activated shell instead of prefixing every command with `uv run`?
`source .venv/bin/activate` still works — `uv sync` manages that same `.venv`.

- API Documentation: [http://localhost:8000/docs](http://localhost:8000/docs)
- Health Endpoint: [http://localhost:8000/health](http://localhost:8000/health)

---

## 🧪 Running Tests

```bash
uv run pytest -v
```

Integration tests spin up an in-memory SQLite DB per test (see `tests/conftest.py`) and drive the API
through `httpx.AsyncClient` — no `.env`/local DB setup required to run them.

## 📚 API Overview

See `/docs` for the full interactive schema. Resource groups: `auth`, `users`, `households` (incl.
invites and member roles), `budgets`, `categories`, `transactions`, and a per-household `dashboard` +
`activity-feed`. Auth is email + 6-digit PIN (JWT bearer token thereafter) — PIN/invite delivery is
currently a log-only stub, see [AGENTS.md](../AGENTS.md#known-gaps-deliberately-deferred-see-pr-discussion).
