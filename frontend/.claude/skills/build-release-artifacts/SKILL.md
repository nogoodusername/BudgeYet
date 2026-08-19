---
name: build-release-artifacts
description: Build the budge-yet frontend release artifacts for Play Console upload and SideStore sideloading — a signed, R8-minified Android App Bundle (AAB) with its deobfuscation mapping, and an unsigned iOS .ipa. Use when the user asks to build the release AAB / .aab for Play Store upload, or the unsigned iOS app package for SideStore. Verify signing, capture the mapping file, and save artifacts to frontend/release-artifacts/.
---

# Release artifacts — Android AAB (Play Console) + unsigned iOS .ipa (SideStore)

Produces two artifacts for `frontend/release-artifacts/`:
- `composeApp-release.aab` — signed, R8-minified Android App Bundle for Google Play, plus `mapping-<VERSION_NAME>-<VERSION_CODE>.txt` (deobfuscation file, must be uploaded alongside the AAB).
- `budgeyet.ipa` — unsigned iOS app for SideStore re-signing.

**Use the AAB for Play, never an APK.** Release APK is sideloading/QA only.

## 1. Verify version first

```bash
cat frontend/version.properties     # e.g. VERSION_NAME=1.1.1 / VERSION_CODE=4
```

AAB and mapping build from these values. Confirm the working tree is at the commit you intend to ship.

## 2. Android AAB

```bash
cd frontend
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :composeApp:bundleRelease
```

- **JDK 17 is required** (AGP-compatible). The system default may be a newer JDK — always pass `JAVA_HOME` explicitly.
- `keystore.properties` + `release.jks` are already committed in `frontend/` and wired into `signingConfig "release"` in `composeApp/build.gradle.kts`, so the bundle is automatically signed with the `budgeyet` key (self-signed, no timestamp — normal for Play App Signing).
- Output: `frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab`.

Verify signing + version:

```bash
# Signing cert + expiry (AAB is a JAR, so use jarsigner/keytool, NOT apksigner which only works on APKs):
keytool -printcert -jarfile frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab | grep -E "Owner:|Valid from"
# Expect: Owner: CN=BudgeYet ... / Valid until ~2053

# Version info: the manifest inside an AAB is binary AXML — read from version.properties + the gradle config instead,
# or build a split APK to inspect. The trustworthy source is the build.gradle.kts versionName/versionCode wired to version.properties.
```

## 3. Capture the deobfuscation mapping

The mapping is **per-build** — it changes with every R8-minified build and must match the exact AAB you upload, or Play Console crash reports won't symbolicate.

```bash
cp frontend/composeApp/build/outputs/mapping/release/mapping.txt frontend/release-artifacts/mapping-<VERSION_NAME>-<VERSION_CODE>.txt
```

Upload it at Play Console → your release → **Deobfuscation file**.

## 4. Unsigned iOS .ipa (SideStore)

Build the `.app` for a physical device unsigned, then zip into a `.ipa`:

```bash
cd frontend
export JAVA_HOME=$(/usr/libexec/java_home)
xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -destination 'generic/platform=iOS' \
    -derivedDataPath build/DerivedData \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGN_IDENTITY=- \
    build

rm -rf /tmp/budgeyet-ipa
mkdir -p /tmp/budgeyet-ipa/Payload
cp -R build/DerivedData/Build/Products/Debug-iphoneos/iosApp.app /tmp/budgeyet-ipa/Payload/
mkdir -p build/release-artifacts
cd /tmp/budgeyet-ipa && zip -qry "$OLDPWD/build/release-artifacts/budgeyet.ipa" Payload/ && cd -
```

Delivery: AirDrop `budgeyet.ipa` to the iPhone → SideStore → Install (enable Developer Mode + trust cert on device).

## 5. Stage artifacts

Copy the AAB and mapping into `frontend/release-artifacts/` (gitignored — see `.gitignore` `release-artifacts/`; do NOT commit them):

```bash
mkdir -p frontend/release-artifacts
cp frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab frontend/release-artifacts/
cp frontend/composeApp/build/outputs/mapping/release/mapping.txt frontend/release-artifacts/mapping-<VERSION_NAME>-<VERSION_CODE>.txt
```

## Conventions / gotchas

- **apksigner does not work on AABs** ("Missing AndroidManifest.xml") — use `jarsigner -verify` / `keytool -printcert`.
- JDK 17 path: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Homebrew `openjdk` alias points to a newer major — don't rely on it.
- Release builds are R8-minified (`isMinifyEnabled = true`) — the mapping file is mandatory for crash symbolication.
- Never hand-edit the mapping file or reuse a mapping from a different build.
- The .ipa is **unsigned** by design (SideStore re-signs); do not add signing flags or it can't be sideloaded.
