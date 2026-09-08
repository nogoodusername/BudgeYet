# BudgeYet documentation

Start here, then jump to what you need.

## Using & running

| Doc | For |
|---|---|
| [Getting Started](getting-started.md) | Setting up the backend and frontend for local development |
| [Running on Mobile](running-on-mobile.md) | Building and running on Android emulators/devices and iOS (Xcode, SideStore) |
| [Deployment](deployment.md) | Self‑hosting the backend — one‑command installer, unattended installs, production (Compose + nginx) |
| [Releasing](releasing.md) | Cutting a new frontend release: versioning scheme and flow |

## Understanding the project

| Doc | For |
|---|---|
| [Architecture](architecture.md) | System design, backend layering, data model, containerization, CI/CD |
| [Product Requirements](product-requirements.md) | Scope, goals, personas, roles & permissions, business rules |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | How to propose changes and what CI expects |
| [../AGENTS.md](../AGENTS.md) | Conventions and current state — required reading before changing code (AI agents especially) |

## Frontend deep reference

[`kmp-compose-multiplatform/`](kmp-compose-multiplatform/README.md) is a vendored
Kotlin/Compose Multiplatform guide (architecture, Ktor, navigation, iOS interop,
testing). Where it conflicts with [AGENTS.md](../AGENTS.md), AGENTS.md wins.
