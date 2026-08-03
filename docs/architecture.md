# Technical Architecture Specification — fam-ex

**Version:** 1.0  
**Date:** July 2026  
**Status:** Approved  

---

## 1. System Architecture Overview

`fam-ex` is designed as a modular, decoupled application composed of an asynchronous **FastAPI backend** and a multiplatform **Kotlin Compose Multiplatform (CMP) frontend**. 

```
                                    +--------------------------------+
                                    |     Compose Multiplatform      |
                                    |       Frontend (CMP/KMP)       |
                                    +--------------------------------+
                                       /            |             \
                                      /             |              \
                               +------------+ +------------+ +------------+
                               |  Android   | |    iOS     | | Web (Wasm) |
                               |   Native   | |   Native   | | Browser    |
                               +------------+ +------------+ +------------+
                                      \             |              /
                                       \            |             /
                                    REST/JSON + WebSockets (Ktor Client)
                                                    |
                                                    v
                                    +--------------------------------+
                                    |        FastAPI Backend         |
                                    |   (Python 3.11+, OpenAPI 3.0)  |
                                    +--------------------------------+
                                                    |
                                       SQLAlchemy 2.0 Async ORM
                                                    |
                                       +------------+------------+
                                       |                         |
                                       v                         v
                                +--------------+         +---------------+
                                | Local SQLite |   OR    | Remote Postgres|
                                |  (aiosqlite) |         | (Supabase/    |
                                +--------------+         |  Aiven)       |
                                                         +---------------+
```

---

## 2. Monorepo Design & Repository Layout

The repository follows a single-repo, multi-project structure:

- **Independent Toolchains**: The backend uses standard Python build tools (`pyproject.toml`, `uvicorn`, `alembic`), while the frontend uses Gradle and Kotlin Multiplatform tooling.
- **Isolated CI/CD**: Workflow files in `.github/workflows/` ensure changes in `backend/` trigger backend CI pipelines, and changes in `frontend/` trigger frontend CI pipelines.
- **Shared Contracts**: API routes and request/response shapes are governed by the backend's OpenAPI (Swagger) schema, from which client models/interfaces are mapped into Kotlin.

---

## 3. Backend Architecture (`backend/`)

### 3.1 Framework & Core Design
- **Framework**: FastAPI (built on Starlette and Pydantic v2).
- **Asynchronous I/O**: Fully async handlers with `async` routes and async database drivers (`asyncpg` for Postgres, `aiosqlite` for SQLite).
- **OpenAPI Schema**: Auto-generated interactive documentation exposed at `/docs` (Swagger UI) and `/redoc`.

### 3.2 Database Tier & Setup Selection
The application supports two database drivers configured at setup time via environment variables (`DATABASE_TYPE`):

1. **Local SQLite (`DATABASE_TYPE=sqlite`)**:
   - Connection URL: `sqlite+aiosqlite:///./data/fam_ex.db`
   - Configured with WAL mode (`PRAGMA journal_mode=WAL`) and foreign key enforcement (`PRAGMA foreign_keys=ON`).
   - Ideal for quick local testing, offline development, or embedded lightweight deployments.

2. **Remote PostgreSQL (`DATABASE_TYPE=postgres`)**:
   - Connection URL: `postgresql+asyncpg://<user>:<password>@<host>:<port>/<dbname>`
   - Compatible with **Supabase**, **Aiven**, AWS RDS, or self-hosted PostgreSQL.
   - Utilizes `asyncpg` for high-performance connection pooling.

**First-Time Setup Script (`backend/scripts/setup_env.py`)**:
Developers run `python3 scripts/setup_env.py` to interactively or non-interactively select the database configuration, which generates a customized `.env` file for local development.

### 3.3 Domain Models (SQLAlchemy ORM)
- `users`: User identity, hashed 6-digit PIN auth, display nickname.
- `households`: Household entity, currency preference (`USD`, `EUR`, `INR`, etc.), cycle start day.
- `household_members`: Junction table mapping users to households, with a per-membership role
  (`owner`, `admin`, `member`). Owner is single-holder per household — exactly one member holds it
  at a time, transferred rather than duplicated.
- `budgets`: Monthly budget target per household.
- `categories`: Preset and custom spending categories with assigned monthly limit.
- `transactions`: Expense and income ledger entries with merchant, category, amount, payment mode, and member reference.

---

## 4. Frontend Architecture (`frontend/`)

### 4.1 Kotlin Multiplatform (KMP) & Compose Multiplatform (CMP)
The frontend utilizes a single shared codebase written in Kotlin for domain logic, state management, and UI rendering:

- **`commonMain`**: Shared Compose UI components, navigation, domain models, view models, Ktor HTTP client, and state flows.
- **`androidMain`**: Android `MainActivity`, Android Manifest, and Android platform integrations.
- **`iosMain`**: Kotlin framework export wrapper and iOS `UIViewController` bridges (`MainViewController`).
- **`wasmJsMain`**: WebAssembly (Wasm/JS) entrypoint rendering Compose UI on an HTML Canvas.
- **`iosApp`**: Xcode project linking the compiled `composeApp` framework for iOS execution.

### 4.2 UI Design System & Tokens
The UI implements the **"Stability & Growth"** design system specified in the PRD:
- **Typography**: Manrope font family (Bold for headers/amounts, Medium for labels, Regular for body text).
- **Color Palette**:
  - Slate 900 (`#0f172a`): Main background and primary text.
  - Teal (`#0d9488`): Positive progress, on-track budget indicators.
  - Coral / Warning (`#e11d48`): Over-budget states and alert flags.
- **Layout & Components**: 8px rounded corner cards, linear spend-vs-budget gauge, persistent bottom navigation, and quick-action FAB.

---

## 5. Containerization & Deployment

### 5.1 Docker Architecture (`backend/Dockerfile`)
The backend features a multi-stage Docker build:
1. **Builder Stage**: Installs build tools and python wheels.
2. **Runtime Stage**: Minimal Debian/Python slim image running `uvicorn app.main:app --host 0.0.0.0 --port 8000`.

### 5.2 Docker Compose (`backend/docker-compose.yml`)
Includes service definitions for:
- `backend`: The FastAPI application container.
- `postgres`: Optional local PostgreSQL container for testing production-like setups.

---

## 6. CI/CD Pipeline Design

### 6.1 Backend Pipeline (`.github/workflows/backend-ci.yml`)
- Triggers on push or pull request touching `backend/**`.
- Job 1: Code Linting & Static Analysis (flake8/ruff).
- Job 2: Pytest Execution (runs unit tests against in-memory SQLite).
- Job 3: Docker Build Verification (`docker build`).

### 6.2 Frontend Pipeline (`.github/workflows/frontend-ci.yml`)
- Triggers on push or pull request touching `frontend/**`.
- Job 1: Gradle Build for Android (`:composeApp:assembleDebug`).
- Job 2: Gradle Build for Web Wasm (`:composeApp:wasmJsBrowserDevelopmentRun` / compile).
- Job 3: Gradle Build for iOS Framework (`:composeApp:embedAndSignAppleFrameworkForXcode`).
