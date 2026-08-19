---
name: build-android-debug
description: Build the budge-yet frontend debug APK and run it on the Android emulator. Use when the user asks to build the debug APK, run the app on the Android emulator, install the app on an Android device, or test the Android app in development. Uses the Medium_Phone AVD and the com.imhx.budgeyet applicationId.
---

# Android debug build + run on emulator

Builds the debug APK with the Gradle wrapper and installs/launches it on the local Android emulator (AVD `Medium_Phone`).

## 1. Prereqs

| Item | Verify |
|------|--------|
| Android SDK | `~/Library/Android/sdk` (already set in `frontend/local.properties`) |
| JDK | **JDK 21 (Android Studio JBR)** — this is what the wrapper is configured to use. Default `java` on the machine is JDK 17/26, which will **fail** the Android/KMP build. Always set `JAVA_HOME` to the Studio JBR. |
| AVD | `~/Library/Android/sdk/emulator/emulator -list-avds` → should list `Medium_Phone` |
| adb | `~/Library/Android/sdk/platform-tools/adb` |

JDK 21 JBR path (verify it exists):

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" -version
```

## 2. Start the emulator

```bash
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH
emulator -avd Medium_Phone &        # background launch; omit -no-window to see the UI
adb wait-for-device
```

If the AVD is already booted, `adb devices` shows it as `device` (not `offline`). If you need a headless run for CI, add `-no-window -no-audio -no-boot-anim`.

## 3. Build + install the debug APK

```bash
cd frontend
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$PATH
./gradlew :composeApp:installDebug
```

- Builds `composeApp-debug.apk` (applicationId `com.imhx.budgeyet`, package root `com.budgeyet`) and auto-installs it on the attached device.
- Debug build is **signed with the debug keystore** — no `keystore.properties` involvement.
- Output: `frontend/composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

## 4. Launch the app

```bash
adb shell am start -n com.imhx.budgeyet/com.budgeyet.MainActivity
```

## 5. Verify

```bash
adb shell pidof com.imhx.budgeyet     # prints the PID of the running process
adb shell dumpsys activity activities | grep -i budgeyet   # confirms foreground activity
```

## Conventions / gotchas

- **Always export `JAVA_HOME` to the Studio JBR** for any Android/KMP gradle task on this machine — the Homebrew default (`openjdk@17`) can't build the Kotlin/Android targets reliably.
- The debug APK is separate from the release AAB. Use `bundleRelease` (see **build-release-artifacts**) for Play uploads, never `assembleDebug`/debug APK.
- If `installDebug` fails with "device offline", boot the AVD and run `adb wait-for-device` before retrying.
- ApplicationId `com.imhx.budgeyet` ≠ namespace `com.budgeyet` — use the full applicationId in `adb shell` commands.
