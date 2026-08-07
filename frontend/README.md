# budge-yet Frontend (Compose Multiplatform)

Cross-platform client application for **budge-yet**, built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform (CMP)** for Android, iOS, and Web.

---

## 📱 Multiplatform Target Support

| Target Platform | Source Set | Build Command |
|---|---|---|
| **Android** | `androidMain` | `./gradlew :composeApp:assembleDebug` |
| **iOS** (Device/Simulator) | `iosMain` + `iosApp/` | `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` |
| **Web** (Wasm/JS) | `wasmJsMain` | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |

---

## 🎨 Design System & Theme

The UI follows the **"Stability & Growth"** design system:
- **Typography**: Manrope
- **Colors**: Slate 900 (`#0f172a`), Teal (`#0d9488`), Amber (`#d97706`), Coral (`#e11d48`)
- **Components**: Rounded 8px corners, linear budget progress gauges, card layouts, floating action buttons.

---

## 🚀 Running Targets Locally

### 1. Android
Open `frontend/` in Android Studio, select the `composeApp` run configuration, and launch on an Android emulator or device.

### 2. Web (Wasm)
Run the development server using Gradle:
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
Open `http://localhost:8080` in Chrome/Edge/Firefox.

### 3. iOS
Open `frontend/iosApp/iosApp.xcodeproj` in Xcode, select your simulator target (e.g. iPhone 15), and click **Run**. Xcode automatically triggers the Gradle build step to compile the KMP framework.
