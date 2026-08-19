---
name: release-bump
description: Bump the budge-yet frontend release version using the repo's bump script (changelog, sync script, commit and tag). Use when the user asks to bump the release/version, cut a new release, tag a version, release vX.Y.Z, or prepare a release. Run only from the repo root.
---

# Release version bump — budge-yet frontend

Bumps `frontend/version.properties` (`VERSION_NAME` semver + monotonic `VERSION_CODE`), regenerates derived files, prepends a `CHANGELOG.md` entry, and creates the `release: vX.Y.Z` commit and `vX.Y.Z` tag. Single source of truth: `frontend/version.properties`.

## 1. Prereqs

- Run from the **repo root** (the script errors otherwise).
- Working tree should be clean of anything you don't want in the release commit.
- Confirm the current version before bumping:

```bash
cat frontend/version.properties
git tag | sort -V | tail -5
git log --oneline -5
```

## 2. Run the bump script

```bash
./scripts/bump_version.sh patch "Bug fix: fixed crash on transaction edit"   # 1.1.1 → 1.1.2
./scripts/bump_version.sh minor "Added category search"                      # → 1.2.0
./scripts/bump_version.sh major "Rewrote budget engine"                      # → 2.0.0
```

What it does:
1. Reads `frontend/version.properties`, bumps the requested semver segment, increments `VERSION_CODE` by 1.
2. Regenerates `frontend/iosApp/Config.xcconfig` via `scripts/sync_version.sh`.
3. Prepends `## [X.Y.Z] - YYYY-MM-DD` under `[Unreleased]` in `CHANGELOG.md`.

The script does **not** commit or tag — it prints the commands for you to review and run manually.

## 3. Review the diff

```bash
git diff
git diff frontend/iosApp/Config.xcconfig   # MARKETING_VERSION / CURRENT_PROJECT_VERSION must match version.properties
```

Sanity checks before committing:
- `frontend/version.properties` → new `VERSION_NAME` / `VERSION_CODE` (`VERSION_CODE` incremented by exactly 1).
- `frontend/iosApp/Config.xcconfig` → `MARKETING_VERSION` = new semver, `CURRENT_PROJECT_VERSION` = new code. This is enforced by the `version-drift-check` CI job — if they drift, CI fails.
- `CHANGELOG.md` → new dated entry present, `[Unreleased]` intact above it.

## 4. Commit and tag

```bash
git add frontend/version.properties frontend/iosApp/Config.xcconfig CHANGELOG.md
git commit -m "release: vX.Y.Z"
git tag vX.Y.Z
git push origin main --tags
```

Only stage the three files — nothing else (build artifacts are gitignored via `release-artifacts/`).

## 5. Post-bump (if the user wants artifacts)

Point to the **build-android-release** (AAB + mapping) or **build-ios-release** (unsigned `.ipa` for SideStore) skills. Both read `version.properties` directly, so a rebuild picks up the new version automatically.

## Conventions / gotchas

- Do **not** bump if HEAD is already tagged with the requested version — ask the user first.
- `VERSION_CODE` must stay monotonic across all platforms (shared by Android `versionCode` and iOS `CURRENT_PROJECT_VERSION`). Never hand-decrement.
- If `frontend/version.properties` was edited by hand, run `./scripts/sync_version.sh` manually to regenerate `Config.xcconfig`.
- Never edit `frontend/iosApp/Config.xcconfig` by hand as the version source — it is derived.
