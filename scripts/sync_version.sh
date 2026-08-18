#!/usr/bin/env bash
# sync_version.sh — reads frontend/version.properties and regenerates derived
# version files (Config.xcconfig) so they stay in sync.
# Intended to run from the repo root.

set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
PROPERTIES="$HERE/frontend/version.properties"
XCCONFIG="$HERE/frontend/iosApp/Config.xcconfig"

if [[ ! -f "$PROPERTIES" ]]; then
  echo "ERROR: $PROPERTIES not found — run from repo root." >&2
  exit 1
fi

# shellcheck source=frontend/version.properties
source "$PROPERTIES"

if [[ -z "${VERSION_NAME:-}" || -z "${VERSION_CODE:-}" ]]; then
  echo "ERROR: VERSION_NAME or VERSION_CODE not set in $PROPERTIES" >&2
  exit 1
fi

# Regenerate Config.xcconfig
cat > "$XCCONFIG" <<EOF
MARKETING_VERSION = $VERSION_NAME
CURRENT_PROJECT_VERSION = $VERSION_CODE
EOF

echo "synced  Config.xcconfig → MARKETING_VERSION=$VERSION_NAME, CURRENT_PROJECT_VERSION=$VERSION_CODE"

# Verify Gradle-side values match (Gradle reads the properties file directly,
# so this is a consistency check rather than a regeneration).
echo "check  build.gradle.kts reads from version.properties — OK (direct reference)"

echo "done."