# Releasing

This doc describes how to cut a new release of the **budge-yet** frontend (Android / iOS / Web).

## Versioning scheme

- **Version name** — semantic versioning `MAJOR.MINOR.PATCH` (e.g. `1.1.0`). Displayed to users on all platforms.
- **Version code** — monotonically increasing integer, shared across all three platforms (used for Android `versionCode` and iOS `CURRENT_PROJECT_VERSION`).
- **Git tag** — `vMAJOR.MINOR.PATCH` created at release time.

## Single source of truth

All version numbers live in [`frontend/version.properties`](../frontend/version.properties):

```properties
VERSION_NAME=1.0.1
VERSION_CODE=2
```

### How each platform reads it

| Platform | Mechanism |
|----------|-----------|
| Android  | `composeApp/build.gradle.kts` reads `version.properties` directly via Java `Properties` |
| iOS      | `iosApp/Config.xcconfig` is regenerated from `version.properties` by `scripts/sync_version.sh`. Xcode's build configs are based on this xcconfig. |
| Web      | A Gradle task (`generateAppVersion`) reads `version.properties` and generates `AppVersion.kt` at build time, exposing `AppVersion.VERSION_NAME` / `AppVersion.VERSION_CODE` in common source. |

## Bumping the version

Use the bump script from the repo root:

```bash
# Bump patch (1.0.1 → 1.0.2)
./scripts/bump_version.sh patch "Bug fix: fixed crash on transaction edit"

# Bump minor (1.0.1 → 1.1.0)
./scripts/bump_version.sh minor "Added category search"

# Bump major (1.0.1 → 2.0.0)
./scripts/bump_version.sh major "Rewrote budget engine"
```

What it does:
1. Reads `frontend/version.properties`, bumps the requested semver segment, increments `VERSION_CODE` by 1.
2. Writes new values back to `version.properties`.
3. Runs `scripts/sync_version.sh` to regenerate `frontend/iosApp/Config.xcconfig`.
4. Prepends a `## [X.Y.Z] - YYYY-MM-DD` entry to `CHANGELOG.md`.
5. Prints the `git add` / `git commit` / `git tag` commands for you to review and run manually.

The script does **not** commit or tag automatically — inspect the diff first:

```bash
git diff
```

## Manual sync

If you ever edit `frontend/version.properties` by hand, run the sync script to regenerate `Config.xcconfig`:

```bash
./scripts/sync_version.sh
```

## Checklist

Before releasing:

1. [ ] Run `./scripts/sync_version.sh` and confirm it reports no errors.
2. [ ] Verify `frontend/iosApp/Config.xcconfig` has the right version.
3. [ ] Build Android: `cd frontend && ./gradlew :composeApp:assembleDebug` — check that the APK's version name/code match.
4. [ ] Build iOS (optional, macOS + Xcode required): `xcodebuild -project frontend/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build`
5. [ ] Build Web: `cd frontend && ./gradlew :composeApp:jsBrowserProductionWebpack` — confirm the dist bundle exists.
6. [ ] Review `CHANGELOG.md` for completeness.
7. [ ] Commit, tag, and push:
     ```bash
     git add frontend/version.properties frontend/iosApp/Config.xcconfig CHANGELOG.md
     git commit -m "release: vX.Y.Z"
     git tag vX.Y.Z
     git push origin main --tags
     ```

## CI drift check

The `version-drift-check` job in `.github/workflows/frontend-ci.yml` reads `frontend/version.properties` and verifies that `frontend/iosApp/Config.xcconfig` has matching `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`. If they drift (e.g. someone bumps Android's version but forgets iOS), CI fails with a clear message.

Android can't drift — `build.gradle.kts` reads `version.properties` directly.

## Crash & performance monitoring (Sentry)

budge-yet uses [Sentry](https://sentry.io) for crash reporting and performance tracing across all
three platforms. The SDK is initialized once in [`App.kt`](../frontend/composeApp/src/commonMain/kotlin/com/budgeyet/App.kt)
from `AppVersion.VERSION_NAME` (the release) and a hardcoded DSN (`core/monitoring/SentryConfig.kt`).
A single Sentry project receives events from all targets; each event is tagged with its `platform`
(`android` / `ios` / `javascript`) so they can be filtered per-platform in the Sentry UI.

### Symbol/upload setup (one time)

1. Create `frontend/composeApp/sentry.properties` from the committed
   `frontend/composeApp/sentry.properties.example`, filling in:
   - `defaults.org=imhx` (fixed)
   - `defaults.project=<your Sentry project slug>` — the project slug under Settings → Projects.
   - `auth.token` — a Sentry auth token with `project:releases` + `org:read` (the CI secret
     `SENTRY_AUTH_TOKEN` can substitute for this env var instead).
2. **Android** — nothing else needed: the Sentry Kotlin Multiplatform Gradle plugin auto-applies the
   Sentry Android Gradle Plugin to the Android target, which uploads the R8 `mapping.txt` automatically
   during the release build:
   ```bash
   cd frontend && ./gradlew :composeApp:bundleRelease   # mapping.txt uploads to Sentry
   ```
   The upload needs `composeApp/sentry.properties` (or `SENTRY_AUTH_TOKEN` in the env) to be present
   at build time.
3. **iOS** — dSYM upload is **not** wired into the Gradle build. After producing the release archive,
   upload symbols with `sentry-cli` (install via `npm i -g @sentry/cli` or Homebrew):
   ```bash
   export SENTRY_AUTH_TOKEN=<token>
   sentry-cli debug-files upload --org imhx --project <slug> \
       frontend/iosApp/build                 # adjust to the dSYM output dir
   ```
   Alternatively add a "Run Script" build phase to `iosApp.xcodeproj` that invokes `sentry-cli
   debug-files upload` with `SENTRY_DSN` set in the scheme.
4. **Web** — `@sentry/browser` reports source-mapped errors automatically; upload the webpack source
   map alongside the bundle if you want readable stack traces:
   ```bash
   sentry-cli sourcemaps upload --org imhx --project <slug> \
       frontend/composeApp/build/dist/js/productionExecutable
   ```

### Force-testing a crash

To confirm ingestion, temporarily call `getMonitoring().captureException(RuntimeException("test crash"))`
from a button (e.g. a debug-only Settings entry) and watch the Issue appear in Sentry within a minute.
