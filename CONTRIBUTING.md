# Contributing to BudgeYet

Thanks for your interest. Issues and pull requests are welcome.

## Before you start

- Read [AGENTS.md](AGENTS.md) — it captures the current state of the codebase,
  the conventions each side follows, and deliberately deferred gaps. It's the
  fastest way to avoid re‑litigating settled decisions.
- Product behaviour (roles, limits, cycle/rollover rules) is specified in
  [docs/product-requirements.md](docs/product-requirements.md). Don't guess at
  business rules.
- This is a monorepo with two independent toolchains. Only touch the one your
  change needs — CI is path‑gated on `backend/**` and `frontend/**`.

## Local setup

See [docs/getting-started.md](docs/getting-started.md).

## Conventions

### Backend (`backend/`)

- Strict layering: **Router → Controller → Service → Repository** (`app/api/` →
  `app/controllers/` → `app/services/` → `app/repositories/`).
- All route handlers are `async def`; use the async session from
  `api/deps.get_db`.
- SQLAlchemy 2.0 `Mapped[...]` / `mapped_column` style, integer autoincrement PKs.
- Any schema change needs an Alembic migration in `alembic/versions/` — nothing
  calls `create_all`. Money columns are `Numeric(12, 2)` / `Decimal`, never float.
- Run before pushing:

  ```bash
  cd backend
  uv run ruff check app/
  uv run pytest -v
  ```

### Frontend (`frontend/`)

- Put new feature code in `commonMain/` by default; drop into a platform source
  set only for genuine platform APIs.
- MVVM‑ish: Controller holds state, screens are stateless composables.
- The toolchain is pinned (Kotlin 1.9.23, Compose MP 1.6.1, Ktor 2.3.9, AGP
  8.2.2, JDK 17) — avoid adding dependencies that would force a bump.
- Run before pushing:

  ```bash
  cd frontend
  ./gradlew :composeApp:assembleDebug
  ./gradlew :composeApp:iosSimulatorArm64Test
  ```

## Commits & PRs

- Keep PRs scoped to one concern. Backend‑only or frontend‑only where possible.
- Describe the user‑visible change and how you verified it. If tests fail or a
  step was skipped, say so.
- Update [CHANGELOG.md](CHANGELOG.md) under `## [Unreleased]` for user‑facing
  changes.
- Update [AGENTS.md](AGENTS.md) when you change something it describes.

## Releases

Handled by maintainers via `scripts/bump_version.sh` — see
[docs/releasing.md](docs/releasing.md).
