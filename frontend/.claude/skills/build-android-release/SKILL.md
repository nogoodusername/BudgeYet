---
name: build-android-release
description: Build the budge-yet frontend Android release App Bundle (AAB) for Google Play Console upload, with its matching deobfuscation mapping file. Use when the user asks to build the release AAB / .aab for Play Store, package an Android release for Play, or prepare Android Play Console artifacts. Verifies signing, captures the per-build mapping, and stages artifacts in frontend/release-artifacts/.
---

# Android release — AAB for Google Play Console

Builds the signed, R8-minified Android App Bundle and its matching deobfuscation mapping. **Use the AAB for Play, never an APK** — the release APK (`assembleRelease`) is sideloading/QA only.

## 1. Verify version first

```bash
cat frontend/version.properties     # e.g. VERSION_NAME=1.1.1 / VERSION_CODE=4
```

The AAB and mapping build from these values (wired into `composeApp/build.gradle.kts` `versionName`/`versionCode`). Confirm the working tree is at the commit you intend to ship.

## 2. Build the AAB

```bash
cd frontend
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :composeApp:bundleRelease
```

- **JDK 17 is required** (AGP-compatible). The system default may be a newer JDK — always pass `JAVA_HOME` explicitly. Path: `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
- `keystore.properties` + `release.jks` are already committed in `frontend/` and wired into `signingConfig "release"` in `composeApp/build.gradle.kts`, so the bundle is automatically signed with the `budgeyet` key (self-signed, no timestamp — normal for Play App Signing).
- Output: `frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab`.

## 3. Verify signing

The AAB is a JAR, so use `jarsigner`/`keytool` — **`apksigner` does not work on AABs** (fails with "Missing AndroidManifest.xml"; it only reads APKs).

```bash
keytool -printcert -jarfile frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab | grep -E "Owner:|Valid from"
# Expect: Owner: CN=BudgeYet ... / Valid until ~2053
```

Version info: the manifest inside an AAB is binary AXML, so it can't be read directly — trust `version.properties` + the gradle `versionName`/`versionCode` wiring instead.

## 4. Capture the deobfuscation mapping

The mapping is **per-build** — it changes with every R8-minified build and must match the exact AAB you upload, or Play Console crash reports won't symbolicate.

```bash
mkdir -p frontend/release-artifacts
cp frontend/composeApp/build/outputs/mapping/release/mapping.txt frontend/release-artifacts/mapping-<VERSION_NAME>-<VERSION_CODE>.txt
```

Upload it at Play Console → your release → **Deobfuscation file**.

## 5. Stage the AAB

```bash
cp frontend/composeApp/build/outputs/bundle/release/composeApp-release.aab frontend/release-artifacts/
```

`frontend/release-artifacts/` is gitignored — these are per-build outputs, do **not** commit them.

## Conventions / gotchas

- Release builds are R8-minified (`isMinifyEnabled = true`) + resource shrinking — the mapping file is mandatory for crash symbolication.
- Never hand-edit the mapping file or reuse a mapping from a different build — each AAB needs its own.
- If `bundleRelease` reports everything `UP-TO-DATE`, the AAB is current with HEAD — still re-copy the mapping + AAB to `release-artifacts/` so the pair on disk matches.
- The AAB is signed with the release key. To inspect the resulting version, build the debug/split APK from the same config, or rely on `version.properties`.
