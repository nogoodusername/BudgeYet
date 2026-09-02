# Production deployment (single VPS, Docker Compose + nginx)

This branch (`prod`) tracks the server-only files that are **not** on `main`:

| File | Purpose |
|------|---------|
| `backend/docker-compose.prod.yml` | Postgres-backed stack: `backend`, `login-cleanup`, `nginx` (TLS terminator). No DB container — Postgres is external (see `POSTGRES_SERVER`). |
| `backend/nginx.conf` | Reverse proxy for `budgeyet-api.imhx.top`, HTTP→HTTPS redirect, Cloudflare origin cert. |

Keep this branch rebased on / merged from `main` so the app code stays current.

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
cd ~/budgeyet && git pull
cd backend
docker compose -f docker-compose.prod.yml up -d --build --force-recreate
docker compose -f docker-compose.prod.yml exec backend alembic upgrade head
```

`--force-recreate` is required after any `.env` change — `docker compose restart` reuses the
old environment and silently keeps stale values (this is how email delivery broke once:
`RESEND_API_KEY` was set in `.env` but never reached the running container).

## Verify email

```bash
docker compose -f docker-compose.prod.yml exec backend printenv RESEND_API_KEY
docker compose -f docker-compose.prod.yml logs -f backend | grep -iE "resend|stub email"
```

- `Resend send ok to=…`  → working
- `STUB EMAIL to=…`       → `RESEND_API_KEY` empty in container (recreate)
- `Resend send failed … status=403` → `EMAIL_FROM_ADDRESS` domain not verified in Resend
