# fam-ex Backend Service

FastAPI-powered REST API backend for the `fam-ex` collaborative household budget application.

---

## 🛠️ Tech Stack & Key Features
- **FastAPI**: Modern, high-performance web framework for Python 3.11+.
- **OpenAPI 3.0**: Auto-generated API documentation accessible at `/docs`.
- **SQLAlchemy 2.0 Async ORM**: Asynchronous data access layer.
- **Database Driver Choice**: Configurable at setup time for **Local SQLite (`aiosqlite`)** or **PostgreSQL (`asyncpg`)** (Supabase / Aiven / Local Postgres).
- **Alembic**: Database migration management.
- **Docker**: Containerized deployment for local development and production.

---

## ⚙️ Initial Project Setup & Database Driver Choice

Run the setup helper script to configure your environment and database engine choice:

```bash
# Interactive setup
python3 scripts/setup_env.py

# Non-interactive SQLite setup
python3 scripts/setup_env.py sqlite

# Non-interactive Postgres setup
python3 scripts/setup_env.py postgres
```

This generates `.env` with the chosen database configuration.

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

---

## 💻 Running Locally without Docker

```bash
# Create virtual environment
python3 -m venv .venv
source .venv/bin/activate

# Install dependencies in editable mode
pip install -e ".[dev]"

# Run FastAPI server
uvicorn app.main:app --reload --port 8000
```

- API Documentation: [http://localhost:8000/docs](http://localhost:8000/docs)
- Health Endpoint: [http://localhost:8000/health](http://localhost:8000/health)
