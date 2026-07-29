#!/usr/bin/env bash
# fam-ex backend installer — clones the repo, configures the environment, and
# starts the API via Docker Compose. Designed to run standalone on a fresh
# server with nothing but git, docker, and python3 already present:
#
#   curl -fsSL https://raw.githubusercontent.com/nogoodusername/fam-ex/main/scripts/install.sh | bash
#
# Non-interactive/automation use (Ansible, cloud-init, CI, ...):
#
#   curl -fsSL .../install.sh | bash -s -- --yes --db sqlite
#   curl -fsSL .../install.sh | bash -s -- --yes --db postgres \
#       --postgres-password 'a-real-password'
#
# Run `install.sh --help` for the full flag/env-var reference.

set -euo pipefail

# ---------------------------------------------------------------------------
# Defaults (overridable via flags or env vars)
# ---------------------------------------------------------------------------
REPO_URL="${FAMEX_REPO:-https://github.com/nogoodusername/fam-ex.git}"
BRANCH="${FAMEX_BRANCH:-main}"
INSTALL_DIR="${FAMEX_DIR:-fam-ex}"
NON_INTERACTIVE="${FAMEX_NONINTERACTIVE:-0}"
DB_CHOICE="${FAMEX_DB:-}"                      # sqlite | postgres
POSTGRES_USER_IN="${POSTGRES_USER:-}"
POSTGRES_PASSWORD_IN="${POSTGRES_PASSWORD:-}"
POSTGRES_DB_IN="${POSTGRES_DB:-}"
RECONFIGURE=0

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
warn()    { printf '%s[!]%s %s\n' "$C_WARN" "$C_RESET" "$1"; }
die()     { printf '%s[x]%s %s\n' "$C_ERR" "$C_RESET" "$1" >&2; exit 1; }

usage() {
  cat <<'EOF'
fam-ex backend installer

Usage: install.sh [options]

Options:
  --dir PATH               Install into PATH (default: ./fam-ex)
  --branch NAME             Git branch/tag to clone (default: main)
  --repo URL                 Repository URL (default: upstream GitHub repo)
  --db sqlite|postgres      Database backend; skips the interactive prompt
  --postgres-user NAME       Postgres user (postgres db only)
  --postgres-password PASS  Postgres password (postgres db only; random if omitted)
  --postgres-db NAME         Postgres database name (default: fam_ex)
  --reconfigure               Regenerate backend/.env even if one already exists
  --yes, --non-interactive  Never prompt; use flags/env-var values or defaults
  -h, --help                  Show this help and exit

Environment variables (same effect as the matching flag):
  FAMEX_REPO, FAMEX_BRANCH, FAMEX_DIR, FAMEX_DB, FAMEX_NONINTERACTIVE,
  POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --dir) INSTALL_DIR="$2"; shift 2 ;;
    --branch) BRANCH="$2"; shift 2 ;;
    --repo) REPO_URL="$2"; shift 2 ;;
    --db) DB_CHOICE="$2"; shift 2 ;;
    --postgres-user) POSTGRES_USER_IN="$2"; shift 2 ;;
    --postgres-password) POSTGRES_PASSWORD_IN="$2"; shift 2 ;;
    --postgres-db) POSTGRES_DB_IN="$2"; shift 2 ;;
    --reconfigure) RECONFIGURE=1; shift ;;
    --yes|--non-interactive) NON_INTERACTIVE=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown option: $1 (see --help)" ;;
  esac
done

# When piped through `curl | bash`, stdin is the script itself, not the
# keyboard — reopen it from the controlling terminal so `read` prompts work.
if [ "$NON_INTERACTIVE" != "1" ] && [ ! -t 0 ]; then
  if [ -e /dev/tty ]; then
    exec < /dev/tty
  else
    warn "No TTY available to prompt on — falling back to non-interactive defaults."
    NON_INTERACTIVE=1
  fi
fi

# ---------------------------------------------------------------------------
# Prerequisite checks
# ---------------------------------------------------------------------------
require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not found. $2"
}

require_cmd git "Install it via your package manager (e.g. apt install git / yum install git)."
require_cmd python3 "Install it via your package manager (e.g. apt install python3)."
require_cmd docker "Install Docker first: https://docs.docker.com/engine/install/"

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  die "Docker Compose is required (either the 'docker compose' plugin or standalone 'docker-compose'). See https://docs.docker.com/compose/install/"
fi

docker info >/dev/null 2>&1 || die "Docker is installed but not running/reachable (check permissions or start the daemon)."

# ---------------------------------------------------------------------------
# Clone (or update an existing clone)
# ---------------------------------------------------------------------------
if [ -d "$INSTALL_DIR/.git" ]; then
  info "Found an existing clone at '$INSTALL_DIR'."
  ORIGIN_URL="$(git -C "$INSTALL_DIR" remote get-url origin 2>/dev/null || echo "")"
  if [ "$ORIGIN_URL" != "$REPO_URL" ]; then
    die "'$INSTALL_DIR' exists and is not a clone of $REPO_URL (origin: ${ORIGIN_URL:-none}). Remove it or pass --dir to choose a different path."
  fi

  do_update=1
  if [ "$NON_INTERACTIVE" != "1" ]; then
    read -r -p "Update it to the latest '$BRANCH' now? Any local changes in that directory will be discarded. [Y/n] " reply
    case "$reply" in
      [nN]*) do_update=0 ;;
    esac
  fi

  if [ "$do_update" = "1" ]; then
    info "Updating '$INSTALL_DIR' to latest '$BRANCH'..."
    git -C "$INSTALL_DIR" fetch --depth 1 origin "$BRANCH"
    git -C "$INSTALL_DIR" checkout "$BRANCH"
    git -C "$INSTALL_DIR" reset --hard "origin/$BRANCH"
  else
    info "Skipping update; using the code already in '$INSTALL_DIR'."
  fi
elif [ -e "$INSTALL_DIR" ]; then
  die "'$INSTALL_DIR' already exists and isn't a git clone. Remove it or pass --dir to choose a different path."
else
  info "Cloning $REPO_URL (branch: $BRANCH) into '$INSTALL_DIR'..."
  git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$INSTALL_DIR"
fi

BACKEND_DIR="$INSTALL_DIR/backend"
[ -d "$BACKEND_DIR" ] || die "Expected a 'backend/' directory in the clone but didn't find one."
cd "$BACKEND_DIR"

# ---------------------------------------------------------------------------
# Database backend choice
# ---------------------------------------------------------------------------
if [ -f .env ] && [ "$RECONFIGURE" != "1" ]; then
  info "Existing backend/.env found — leaving it as-is (pass --reconfigure to regenerate it)."
  SKIP_ENV_SETUP=1
else
  SKIP_ENV_SETUP=0

  if [ -z "$DB_CHOICE" ]; then
    if [ "$NON_INTERACTIVE" = "1" ]; then
      DB_CHOICE="sqlite"
    else
      echo
      echo "Which database backend should this deployment use?"
      echo "  [1] SQLite      — simplest, file-based, no extra container"
      echo "  [2] PostgreSQL  — runs a bundled 'postgres' container alongside the API"
      read -r -p "Enter choice [1 or 2] (default: 1): " db_reply
      case "$db_reply" in
        2) DB_CHOICE="postgres" ;;
        *) DB_CHOICE="sqlite" ;;
      esac
    fi
  fi

  case "$DB_CHOICE" in
    sqlite|postgres) ;;
    *) die "--db must be 'sqlite' or 'postgres' (got '$DB_CHOICE')" ;;
  esac
fi

# ---------------------------------------------------------------------------
# Generate backend/.env (skipped above if one already exists)
# ---------------------------------------------------------------------------
random_password() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 16
  else
    head -c 16 /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

if [ "$SKIP_ENV_SETUP" != "1" ]; then
  if [ "$DB_CHOICE" = "postgres" ]; then
    POSTGRES_USER="${POSTGRES_USER_IN:-postgres}"
    POSTGRES_DB="${POSTGRES_DB_IN:-fam_ex}"

    if [ -n "$POSTGRES_PASSWORD_IN" ]; then
      POSTGRES_PASSWORD="$POSTGRES_PASSWORD_IN"
    elif [ "$NON_INTERACTIVE" != "1" ]; then
      read -r -p "Postgres user (default: postgres): " reply_user
      POSTGRES_USER="${reply_user:-$POSTGRES_USER}"
      read -r -p "Postgres database name (default: fam_ex): " reply_db
      POSTGRES_DB="${reply_db:-$POSTGRES_DB}"
      read -r -s -p "Postgres password (leave blank to auto-generate): " reply_pw
      echo
      POSTGRES_PASSWORD="${reply_pw:-$(random_password)}"
    else
      POSTGRES_PASSWORD="$(random_password)"
    fi

    export POSTGRES_USER POSTGRES_PASSWORD POSTGRES_DB
    info "Writing backend/.env for PostgreSQL (db: $POSTGRES_DB, user: $POSTGRES_USER)..."
    python3 scripts/setup_env.py postgres
    success "Generated Postgres password: $POSTGRES_PASSWORD (also saved in backend/.env — keep this safe)"
  else
    info "Writing backend/.env for SQLite..."
    python3 scripts/setup_env.py sqlite
  fi
fi

# ---------------------------------------------------------------------------
# Determine which backend this deployment is actually configured for (in case
# an existing .env was kept as-is above) and pick the matching compose file.
# ---------------------------------------------------------------------------
CONFIGURED_DB="$(grep -E '^DATABASE_TYPE=' .env | tail -n1 | cut -d= -f2)"
if [ "$CONFIGURED_DB" = "postgres" ]; then
  COMPOSE_FILE="docker-compose.yml"
else
  COMPOSE_FILE="docker-compose.sqlite.yml"
fi

# ---------------------------------------------------------------------------
# Start the stack
# ---------------------------------------------------------------------------
info "Starting the backend via '${COMPOSE_CMD[*]} -f $COMPOSE_FILE' (this builds the image on first run)..."
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up --build -d

# ---------------------------------------------------------------------------
# Wait for /health
# ---------------------------------------------------------------------------
info "Waiting for the API to become healthy..."
HEALTHY=0
for _ in $(seq 1 30); do
  if curl -fsS "http://localhost:8000/health" >/dev/null 2>&1; then
    HEALTHY=1
    break
  fi
  sleep 2
done

if [ "$HEALTHY" != "1" ]; then
  warn "API didn't respond on /health within 60s. Check logs with:"
  warn "  (cd $BACKEND_DIR && ${COMPOSE_CMD[*]} -f $COMPOSE_FILE logs -f)"
  exit 1
fi

# Best-effort public IP for the summary — never fail the install over this.
PUBLIC_IP="$(curl -fsS --max-time 2 https://ifconfig.me 2>/dev/null || true)"

echo
success "fam-ex backend is up (database: $CONFIGURED_DB)."
echo "  Local:    http://localhost:8000/docs"
[ -n "$PUBLIC_IP" ] && echo "  Public:   http://$PUBLIC_IP:8000/docs  (open port 8000 in your firewall/security group if needed)"
echo
echo "Useful commands (run from $BACKEND_DIR):"
echo "  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE logs -f      # tail logs"
echo "  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE down          # stop"
echo "  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE up --build -d # rebuild & restart after an update"
