# Production deployment (single VPS, Docker Compose + nginx)

The server is deployed from the `backend/production` branch (kept in sync with `main`). The
prod-only pieces of the stack:

| File | Purpose |
|------|---------|
| `backend/docker-compose.prod.yml` | Postgres-backed stack: `backend`, `login-cleanup`, `nginx` (TLS terminator). No DB container — Postgres is external (see `POSTGRES_SERVER`). |
| `backend/nginx.conf` | Reverse proxy for `budgeyet-api.imhx.top`, HTTP→HTTPS redirect, Cloudflare origin cert. |
| `backend/scripts/deploy.sh` | One-command redeploy: pull, build, migrate, recreate, health-gate, rollback-on-failure. |

## Host layout

```
~/budgeyet/                     # git checkout of this repo
/etc/famex/certs/               # Cloudflare origin cert (NOT in git)
  origin-cert.pem
  origin-key.pem
```

## backend/.env (NOT committed — recreate by hand)

Compose reads `backend/.env` for `${VAR}` interpolation. Required keys:

```
DATABASE_TYPE=postgres
POSTGRES_SERVER=<db host>
POSTGRES_PORT=5432
POSTGRES_USER=<user>
POSTGRES_PASSWORD=<secret>
POSTGRES_DB=fam_ex
POSTGRES_SSL=true            # if the managed DB requires it
SECRET_KEY=<64 hex chars>    # JWT signing key
RESEND_API_KEY=re_xxx        # outbound email; blank = stub/log-only mode. NO QUOTES.
EMAIL_FROM_ADDRESS=noreply@notify.imhx.top   # must be a Resend-verified domain
EMAIL_FROM_NAME=BudgeYet
CORS_ORIGINS=<cloudflare pages domain>,http://localhost:8080,http://127.0.0.1:8080,http://localhost:3000
```

## Deploy / redeploy

```bash
~/budgeyet/backend/scripts/deploy.sh
```

`deploy.sh` fetches the checked-out branch, rebuilds the image, runs `alembic upgrade heads`
against the external DB **before** restarting, recreates the stack with `--force-recreate`,
then polls `/health` and rolls the *code* back if the DB never reports healthy. A migration
that already applied is **not** auto-reverted — snapshot the DB (or use the managed provider's
point-in-time restore) before deploying a destructive schema change.

- `FORCE=1 ~/budgeyet/backend/scripts/deploy.sh` — redeploy with no new commits.
- `DEPLOY_BRANCH=<branch> ~/budgeyet/backend/scripts/deploy.sh` — deploy a different branch.

`--force-recreate` is required after any `.env` change — `docker compose restart` reuses the
old environment and silently keeps stale values (this is how email delivery broke once:
`RESEND_API_KEY` was set in `.env` but never reached the running container).

Manual equivalent, if you need to run the steps by hand:

```bash
cd ~/budgeyet && git pull
cd backend
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml run --rm backend alembic upgrade heads
docker compose -f docker-compose.prod.yml up -d --force-recreate
```

## Verify email

```bash
docker compose -f docker-compose.prod.yml exec backend printenv RESEND_API_KEY
docker compose -f docker-compose.prod.yml logs -f backend | grep -iE "resend|stub email"
```

- `Resend send ok to=…`  → working
- `STUB EMAIL to=…`       → `RESEND_API_KEY` empty in container (recreate)
- `Resend send failed … status=403` → `EMAIL_FROM_ADDRESS` domain not verified in Resend
