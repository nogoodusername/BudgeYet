---
name: build-ios-debug
description: Build the budge-yet frontend debug iOS app and run it on the iOS Simulator. Use when the user asks to build the debug iOS app, run the app on the iOS simulator, or test the iOS app in development. Uses Xcode, xcodebuild for the iosApp scheme, xcrun simctl to boot/install/launch on a simulator, bundle id com.budgeyet.iosApp.
---

# iOS debug build + run on Simulator

Builds the debug `.app` with `xcodebuild` (linking the Kotlin/Native framework via Gradle) and installs/launches it on a booted iOS Simulator.

## 1. Prereqs

| Item | Verify |
|------|--------|
| Xcode ≥ 15 | `xcode-select -p` → `/Applications/Xcode.app`; `xcodebuild -version` |
| Simulators | `xcrun simctl list devices available` (e.g. iPhone 17, iPhone 16 Pro) |
| JDK | Xcode's build phase invokes Gradle → needs a working JDK. Any recent JDK (17+) is fine here, unlike Android. |

The Xcode project lives at `frontend/iosApp/iosApp.xcodeproj` (scheme `iosApp`, bundle id `com.budgeyet.iosApp`). The app target's debug config runs the Gradle task `linkDebugFrameworkIosArm64` (device) or `linkDebugFrameworkIosX64`/`linkDebugFrameworkIosSimulatorArm64` (simulator) automatically.

## 2. Pick and boot a simulator

```bash
xcrun simctl list devices available
xcrun simctl boot "iPhone 17"        # or use a UUID from the list
open -a Simulator                     # bring the Simulator UI forward (optional)
```

Use a runtime that matches what Xcode supports. If it's already booted, `simctl boot` errors — that's fine, proceed.

## 3. Build the debug app for the simulator

```bash
cd frontend
xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'platform=iOS Simulator,name=iPhone 17' \
    build
```

Output: `frontend/build/Debug-iphonesimulator/iosApp.app` (with `-derivedDataPath build` in `frontend/`, otherwise it lands under `~/Library/Developer/Xcode/DerivedData`).

## 4. Install + launch

```bash
xcrun simctl install booted build/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted com.budgeyet.iosApp
```

## 5. Verify

```bash
xcrun simctl spawn booted launchctl list | grep budgeyet   # running process
# Or visually: the Simulator window shows the app UI.
```

To stream app logs: `xcrun simctl spawn booted log stream --predicate 'process == "iosApp"'`.

## Conventions / gotchas

- Use the **simulator** SDK/destination only for local dev. For a physical iPhone via Xcode, open the project and use automatic signing; simulators need no signing.
- **Unsigned physical-device builds (SideStore)** are a different flow — see the **build-ios-release** skill (`CODE_SIGNING_ALLOWED=NO` + `generic/platform=iOS` + zip to `.ipa`).
- If the build fails at the Gradle phase, the JDK it picked up may be broken — `xcodebuild` inherits `JAVA_HOME`/`PATH`; export `JAVA_HOME=$(/usr/libexec/java_home -v 17)` (or the Studio JBR) before building.
- First simulator build is slow (Kotlin/Native linking). Subsequent builds are incremental.
- Bundle id is `com.budgeyet.iosApp` (not `com.imhx.budgeyet` — that's Android).
