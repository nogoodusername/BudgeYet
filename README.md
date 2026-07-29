# fam-ex — Collaborative Household Budget App

`fam-ex` is a collaborative, cross-platform household budget management application designed for real-time visibility, fast transaction logging, and shared household financial transparency.

---

## 🏗️ Repository Architecture

This repository is structured as a **monorepo** housing both the backend service and the multiplatform frontend clients, each with independent build systems, configuration, and CI/CD pipelines.

```
fam-ex/
├── docs/                             # PRD and Architecture documentation
│   ├── household-budget-app-prd.md   # Product Requirements Document
│   └── architecture.md               # Technical Architecture Specification
├── backend/                          # FastAPI Python Backend Service
│   ├── app/                          # Core application (API, Models, Schemas, Services)
│   ├── alembic/                      # Database migrations
│   ├── scripts/                      # Setup scripts (DB choice: SQLite vs PostgreSQL)
│   ├── Dockerfile                    # Container configuration
│   ├── docker-compose.yml            # Local development compose setup
│   └── pyproject.toml                # Dependencies & package configuration
├── frontend/                         # Kotlin Multiplatform (KMP) & Compose (CMP) UI
│   ├── composeApp/                   # Multiplatform shared module (commonMain, androidMain, iosMain, wasmJsMain)
│   ├── iosApp/                       # Xcode iOS application wrapper
│   ├── build.gradle.kts              # Root Gradle build script
│   └── settings.gradle.kts           # Gradle multiplatform project settings
├── .github/                          # Automated CI/CD Workflows
│   └── workflows/
│       ├── backend-ci.yml            # Backend testing, linting & container build CI
│       └── frontend-ci.yml           # Frontend KMP multi-target build CI
└── README.md                         # Monorepo documentation
```

---

## ⚡ Quick Start

### 1. Backend Setup (`backend/`)

The backend is built with **FastAPI** and **SQLAlchemy 2.0**, with dependencies managed by **[uv](https://docs.astral.sh/uv/)**. At setup time, you can choose between **Local SQLite** or **Remote PostgreSQL (Supabase / Aiven)**.

```bash
cd backend

# Run setup script to configure environment & database driver
uv run python scripts/setup_env.py

# Launch using Docker Compose (Recommended)
docker-compose up --build -d

# Or run locally — uv creates/updates .venv and installs from uv.lock automatically
uv sync
uv run uvicorn app.main:app --reload --port 8000
```

- **OpenAPI / Swagger UI**: [http://localhost:8000/docs](http://localhost:8000/docs)
- **Health Check**: [http://localhost:8000/health](http://localhost:8000/health)

### 2. Frontend Setup (`frontend/`)

The frontend is built using **Compose Multiplatform (CMP)** targeting **Android**, **iOS**, and **Web (Wasm)**.

```bash
cd frontend

# Build Android App
./gradlew :composeApp:assembleDebug

# Run Web App (Wasm/JS)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build KMP Framework for iOS
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

For iOS development, open `frontend/iosApp/iosApp.xcodeproj` in **Xcode** and select your simulator/device target.

---

## 📖 Detailed Documentation

- [Product Requirements Document (PRD)](docs/household-budget-app-prd.md)
- [Technical Architecture Documentation](docs/architecture.md)

---

## 🚀 CI/CD Pipelines

- **Backend CI**: Runs on PR/push to `backend/**`. Performs code formatting check, type checking, pytest tests, and verifies Docker container compilation.
- **Frontend CI**: Runs on PR/push to `frontend/**`. Validates Gradle builds for Android, Web Wasm, and iOS Kotlin framework targets.
