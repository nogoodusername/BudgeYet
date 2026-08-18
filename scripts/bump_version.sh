#!/usr/bin/env bash
# bump_version.sh <major|minor|patch> [changelog-summary]
#
# Bumps VERSION_NAME (semver) and increments VERSION_CODE, regenerates derived
# version files, prepends a CHANGELOG entry, and prints the git commands to
# review and run manually.
#
# Run from the repo root.

set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
PROPERTIES="$HERE/frontend/version.properties"
CHANGELOG="$HERE/CHANGELOG.md"
SYNC_SCRIPT="$HERE/scripts/sync_version.sh"

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

SEGMENT="${1:-}"
if [[ "$SEGMENT" != "major" && "$SEGMENT" != "minor" && "$SEGMENT" != "patch" ]]; then
  echo "Usage: $0 <major|minor|patch> [changelog-summary]" >&2
  exit 1
fi

# Parse semver
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION_NAME"
case "$SEGMENT" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
NEW_CODE=$((VERSION_CODE + 1))

# Write updated version.properties
cat > "$PROPERTIES" <<EOF
VERSION_NAME=$NEW_VERSION
VERSION_CODE=$NEW_CODE
EOF

# Regenerate derived files
bash "$SYNC_SCRIPT"

# Build changelog entry
SUMMARY="${2:-}"
if [[ -z "$SUMMARY" ]]; then
  SUMMARY="Release $NEW_VERSION"
fi

TODAY="$(date +%Y-%m-%d)"
ENTRY="## [$NEW_VERSION] - $TODAY\n### Changed\n- $SUMMARY\n"

if [[ "$(uname)" == "Darwin" ]]; then
  sed -i '' "1,/^## \[Unreleased\]$/s/^## \[Unreleased\]$/## [Unreleased]\n\n$ENTRY/" "$CHANGELOG"
else
  sed -i "1,/^## \[Unreleased\]$/s/^## \[Unreleased\]$/## [Unreleased]\n\n$ENTRY/" "$CHANGELOG"
fi

echo ""
echo "version bumped:  $VERSION_NAME → $NEW_VERSION  (code $NEW_CODE)"
echo ""
echo "Next steps (review then run manually):"
echo ""
echo "  git add \\"
echo "    frontend/version.properties \\"
echo "    frontend/iosApp/Config.xcconfig \\"
echo "    CHANGELOG.md"
echo "  git commit -m \"release: v$NEW_VERSION\""
echo "  git tag v$NEW_VERSION"
echo "  git push origin main --tags"
echo ""