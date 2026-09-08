#!/usr/bin/env bash
# budge-yet backend — production redeploy.
#
# Pulls the latest code for the current branch, rebuilds the image, applies
# Alembic migrations against the external database *before* restarting, recreates
# the stack, then waits for /health to report a healthy DB connection and rolls
# the code back if it never does.
#
# Run on the server from the repo checkout:
#
#   ~/budgeyet/backend/scripts/deploy.sh
#
# Environment overrides:
#   FORCE=1           redeploy even when there are no new commits
#   DEPLOY_BRANCH=x   deploy branch x instead of the checked-out one
#
# See backend/PROD_DEPLOY.md for host layout and the backend/.env key list.

set -euo pipefail

# ---------------------------------------------------------------------------
# Output helpers (color only when writing to a real terminal)
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
  C_INFO=$'\033[36m'; C_OK=$'\033[32m'; C_WARN=$'\033[33m'; C_ERR=$'\033[31m'; C_RESET=$'\033[0m'
else
  C_INFO=""; C_OK=""; C_WARN=""; C_ERR=""; C_RESET=""
fi
info()    { printf '%s[*]%s %s\n' "$C_INFO" "$C_RESET" "$1"; }
success() { printf '%s[+]%s %s\n' "$C_OK" "$C_RESET" "$1"; }
warn()    { printf '%s[!]%s %s\n' "$C_WARN" "$C_RESET" "$1" >&2; }
die()     { printf '%s[x]%s %s\n' "$C_ERR" "$C_RESET" "$1" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Locate the backend/ checkout (parent of this script's scripts/ dir)
# ---------------------------------------------------------------------------
BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"
[ -f docker-compose.prod.yml ] || die "docker-compose.prod.yml not found in $BACKEND_DIR — this isn't the prod checkout."

# ---------------------------------------------------------------------------
# Prerequisites
# ---------------------------------------------------------------------------
require_cmd() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not found."; }
require_cmd git
require_cmd curl
require_cmd docker

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose -f docker-compose.prod.yml)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose -f docker-compose.prod.yml)
else
  die "Docker Compose is required (the 'docker compose' plugin or standalone 'docker-compose')."
fi

# ---------------------------------------------------------------------------
# Work out what we're deploying
# ---------------------------------------------------------------------------
BRANCH="${DEPLOY_BRANCH:-$(git rev-parse --abbrev-ref HEAD)}"
[ "$BRANCH" != "HEAD" ] || die "Detached HEAD — check out the deploy branch or set DEPLOY_BRANCH."

info "Fetching origin/$BRANCH ..."
git fetch --quiet origin "$BRANCH"

OLD="$(git rev-parse HEAD)"
NEW="$(git rev-parse "origin/$BRANCH")"

if [ "$OLD" = "$NEW" ] && [ "${FORCE:-0}" != "1" ]; then
  success "Already at origin/$BRANCH (${OLD:0:8}). Nothing to deploy — set FORCE=1 to redeploy anyway."
  exit 0
fi

info "Deploying ${OLD:0:8} -> ${NEW:0:8} on $BRANCH"
git reset --hard "origin/$BRANCH"

# ---------------------------------------------------------------------------
# Build, then migrate, then restart — in that order.
# ---------------------------------------------------------------------------
info "Building images ..."
"${COMPOSE[@]}" build

# Migrations run in a throwaway container against the external DB *before* the
# app restarts. On failure, set -e aborts here and the currently running
# containers keep serving the old code untouched. `heads` (plural) tolerates a
# briefly-divergent migration graph; see the Alembic note in AGENTS.md.
info "Applying Alembic migrations (alembic upgrade heads) ..."
"${COMPOSE[@]}" run --rm backend alembic upgrade heads

# --force-recreate so a changed backend/.env actually reaches the containers —
# a plain restart reuses the old environment (see PROD_DEPLOY.md).
info "Recreating services ..."
"${COMPOSE[@]}" up -d --force-recreate --remove-orphans

# ---------------------------------------------------------------------------
# Health gate — /health returns HTTP 200 even when the DB is down, so inspect
# the body. `backend` only listens on the compose network, hence `exec`.
# ---------------------------------------------------------------------------
info "Waiting for a healthy database connection ..."
healthy=0
for _ in $(seq 1 30); do
  sleep 2
  body="$("${COMPOSE[@]}" exec -T backend curl -fsS http://localhost:8000/health 2>/dev/null || true)"
  case "$body" in
    *'"database_status":"healthy"'*|*'"database_status": "healthy"'*) healthy=1; break ;;
  esac
done

if [ "$healthy" != "1" ]; then
  warn "Health check failed after ~60s. Last 50 backend log lines:"
  "${COMPOSE[@]}" logs --tail=50 backend >&2 || true
  warn "Rolling code back to ${OLD:0:8} — a migration applied above is NOT reverted, check the DB."
  git reset --hard "$OLD"
  "${COMPOSE[@]}" up -d --build --force-recreate
  die "Deploy failed; rolled back to ${OLD:0:8}."
fi

success "Healthy. Pruning dangling images ..."
docker image prune -f >/dev/null

success "Deploy complete: now at ${NEW:0:8} on $BRANCH."
