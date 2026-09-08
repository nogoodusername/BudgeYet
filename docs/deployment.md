# Deployment

BudgeYet's backend is a single FastAPI service. This covers standing it up on your
own server. The mobile/web clients then point at it via the app's **Backend
Configuration** screen.

- **One command** — for most people, the installer below.
- **Unattended** — flags for Ansible / cloud‑init / CI.
- **Production** — TLS, external Postgres, health‑gated redeploys: see
  [`backend/PROD_DEPLOY.md`](../backend/PROD_DEPLOY.md).

---

## One‑command install

On a fresh Linux/macOS server with `git`, `docker` (with Compose), and `python3`
already present:

```bash
curl -fsSL https://raw.githubusercontent.com/nogoodusername/BudgeYet/main/scripts/install.sh | bash
```

It clones the repo, prompts for a database, generates `backend/.env`, runs
Alembic migrations, and starts the stack with Docker Compose. When it finishes,
the API is live at `:8000/docs`.

### Choosing a database

| Choice | What you get |
|---|---|
| **SQLite** | Single file at `backend/data/`. Simplest; fine for a household. |
| **PostgreSQL** | A bundled Postgres container with credentials generated for you. |

### Re‑running / updating

Run the installer again against the same `--dir` to pull the latest code and
redeploy. It reuses your existing `backend/.env` (secret key, DB credentials)
unless you pass `--reconfigure`.

---

## Unattended install

Skip every prompt:

```bash
curl -fsSL https://raw.githubusercontent.com/nogoodusername/BudgeYet/main/scripts/install.sh | bash -s -- \
  --yes --db sqlite

curl -fsSL https://raw.githubusercontent.com/nogoodusername/BudgeYet/main/scripts/install.sh | bash -s -- \
  --yes --db postgres --postgres-password 'a-real-password'
```

Common flags (`install.sh --help` for the full list, plus `BUDGEYET_*` env‑var
equivalents):

| Flag | Purpose |
|---|---|
| `--dir PATH` | Install location (default `./budge-yet`) |
| `--branch NAME` | Git branch/tag to clone (default `main`) |
| `--repo URL` | Repository URL |
| `--db sqlite\|postgres` | Database backend; skips the prompt |
| `--postgres-user` / `--postgres-password` / `--postgres-db` | Postgres settings (random password if omitted) |
| `--reconfigure` | Regenerate `backend/.env` even if one exists |
| `--yes`, `--non-interactive` | Never prompt |

---

## What the installer sets up

- Clones the repo to `--dir`.
- Drives `backend/scripts/setup_env.py` to write `backend/.env`.
- Picks the matching Compose file (`docker-compose.yml` for Postgres,
  `docker-compose.sqlite.yml` for SQLite) and runs `docker compose up -d`.
- Patches a unique `COMPOSE_PROJECT_NAME` into `.env` so multiple installs on one
  host don't collide.
- Runs `alembic upgrade head` before serving traffic.

`install.sh` reads values back out of the generated `.env`, so if you change
`setup_env.py`'s CLI or either Compose file's name/services/ports, update
`install.sh` to match.

---

## Production

The `backend/production` branch carries the prod‑only pieces:

| File | Purpose |
|---|---|
| `backend/docker-compose.prod.yml` | Postgres‑backed stack (external DB) + `nginx` TLS terminator + login‑cleanup cron |
| `backend/nginx.conf` | Reverse proxy, HTTP→HTTPS redirect, Cloudflare origin cert |
| `backend/scripts/deploy.sh` | One‑command redeploy: pull, build, migrate, recreate, health‑gate, rollback‑on‑failure |

```bash
~/budgeyet/backend/scripts/deploy.sh
```

Full host layout, the required `backend/.env` keys, and email verification steps
are in [`backend/PROD_DEPLOY.md`](../backend/PROD_DEPLOY.md).

### Web client

`.github/workflows/deploy-web-cloudflare.yml` builds the production web bundle
(`jsBrowserProductionWebpack`) and deploys it to Cloudflare Pages on every `v*`
tag. Any static host works — the output is a plain webpack bundle.

---

## Security note

`get_client_ip` trusts `CF-Connecting-IP` / `X-Real-IP` / `X-Forwarded-For`
unconditionally. Behind a CDN that strips client‑supplied versions of those
headers (Cloudflare, etc.) this is fine. On a bare VPS with no proxy, the per‑IP
login rate limit is spoofable — the per‑account lockout still holds. See the
[known gaps](../AGENTS.md#known-gaps-deliberately-deferred-see-pr-discussion).
