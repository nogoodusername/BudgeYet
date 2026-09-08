# BudgeYet Frontend (Compose Multiplatform)

Cross‑platform client for **BudgeYet**, built with **Kotlin Multiplatform (KMP)**
and **Compose Multiplatform (CMP)** for Android, iOS, and Web.

See also: [Getting Started](../docs/getting-started.md) ·
[Running on Mobile](../docs/running-on-mobile.md) ·
[Architecture](../docs/architecture.md) · [AGENTS.md](../AGENTS.md)

---

## Targets

| Platform | Source set | Build command |
|---|---|---|
| **Android** | `androidMain` | `./gradlew :composeApp:assembleDebug` |
| **iOS** | `iosMain` + `iosApp/` | `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, then run `iosApp` in Xcode |
| **Web** | `jsMain` | `./gradlew :composeApp:jsBrowserDevelopmentRun` → <http://localhost:8080> |

The web target is **Kotlin/JS** (`js(IR)`) rendering Compose to an HTML canvas —
not `wasmJs` (Ktor 2.3.9 has no Wasm engine). Production bundle:
`./gradlew :composeApp:jsBrowserProductionWebpack`.

## Design system — "Stability & Growth"

- **Type:** Manrope (bold headers/amounts, medium labels, regular body)
- **Palette:** deep slate (`#0f172a`) · teal for on‑track (`#0d9488` / in‑app `#006B5F`) · amber for near‑limit · coral/red for over‑budget
- **Components:** 8px rounded cards, linear spend‑vs‑budget gauges, persistent bottom nav, quick‑action FAB

Tokens live in `composeApp/src/commonMain/kotlin/com/budgeyet/theme/`.

## Layout

```
composeApp/src/
├── commonMain/   shared Compose UI, state, domain models, Ktor networking  ← most code lives here
├── androidMain/  MainActivity, manifest, Android integrations
├── iosMain/      MainViewController bridge (consumed by iosApp/)
└── jsMain/       browser entrypoint + js(IR) actual implementations
iosApp/           Xcode project that links the compiled KMP framework
```

## Toolchain (pinned)

Kotlin 1.9.23 · Compose Multiplatform 1.6.1 · Ktor 2.3.9 · AGP 8.2.2 · JDK 17.
Use the Gradle wrapper. Avoid dependencies that would force a version bump.

## Point at a backend

Default clients use the hosted API. For a local server, use the app's **Backend
Configuration** screen → **Custom URL** → `http://localhost:8000`.
