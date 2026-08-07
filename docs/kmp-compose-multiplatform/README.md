# KMP + Compose Multiplatform Development Guide

A practical reference for building **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** apps with clean
architecture, Koin DI, Ktor networking, Room KMP persistence, and full Android/iOS/Web coverage — based on Google's
official architecture guidelines (Now in Android) and JetBrains' official documentation.

This guide was originally published as the **Claude Code skill**
[`felipechaux/kmp-compose-multiplatform-skill`](https://github.com/felipechaux/kmp-compose-multiplatform-skill)
(MIT License). It is mirrored here so **any** coding agent (not just Claude) can reference it from this repository.

## Attribution

- **Author:** [felipechaux](https://github.com/felipechaux)
- **Source repository:** https://github.com/felipechaux/kmp-compose-multiplatform-skill
- **License:** MIT — see [LICENSE](https://github.com/felipechaux/kmp-compose-multiplatform-skill/blob/main/LICENSE)
- **Status:** vendored/mirrored from upstream; this repo may have local edits. Re-sync from upstream to refresh.

## What this covers

| Area | Where |
|---|---|
| Architecture (Clean Architecture, feature modules, UDF, inter-feature comms, feature flags, proto DataStore) | [`SKILL.md`](SKILL.md), [`references/architecture.md`](references/architecture.md) |
| Compose UI (Material 3, `@Stable`, state hoisting, focus/a11y, dynamic type, previews, performance) | [`SKILL.md`](SKILL.md), [`references/compose-best-practices.md`](references/compose-best-practices.md) |
| Build system (version catalog, convention plugins, R8/ProGuard, KSP, Maven publishing, CI) | [`references/build-system.md`](references/build-system.md) |
| Error handling (`AppError` hierarchy, `safeApiCall`, recoverable vs fatal, 429, retry) | [`SKILL.md`](SKILL.md), [`references/error-handling.md`](references/error-handling.md) |
| Internationalization (string resources, plurals, RTL, dynamic locale) | [`SKILL.md`](SKILL.md), [`references/i18n.md`](references/i18n.md) |
| iOS interop (Swift naming, SKIE, Kotlin/Native memory model, iOS performance) | [`references/ios-interop.md`](references/ios-interop.md) |
| Navigation (deep links, cross-module contracts, predictive back, transitions) | [`references/navigation.md`](references/navigation.md) |
| Testing (fakes, Turbine, SharedFlow events, Paging, screenshot/golden, Compose UI) | [`references/testing.md`](references/testing.md) |
| DI (Koin multiplatform), Networking (Ktor), Persistence (Room/DataStore), State, Logging | [`SKILL.md`](SKILL.md) |

## How agents should use this

1. **Start with [`SKILL.md`](SKILL.md)** — it is the core instruction set: project structure, layer
   responsibilities, KMP patterns, DI, build system, persistence, networking, i18n, testing, logging,
   and a list of common pitfalls. Follow its conventions when writing or reviewing KMP code.
2. **Drill into a `references/` file** for deep detail on the specific area you're working in.
3. The `references/*.md` links inside `SKILL.md` are relative and resolve correctly within this directory.

## Layout

```
docs/kmp-compose-multiplatform/
├── README.md                  # this file
├── SKILL.md                   # core instruction set
└── references/                # deep-dive guides
    ├── architecture.md
    ├── build-system.md
    ├── compose-best-practices.md
    ├── error-handling.md
    ├── i18n.md
    ├── ios-interop.md
    ├── navigation.md
    └── testing.md
```

## Relation to this repo's frontend

The budge-yet frontend (`frontend/`) is a KMP + Compose Multiplatform project (Kotlin 1.9.23, Compose Multiplatform
1.6.1, Ktor 2.3.9). See [AGENTS.md](../../AGENTS.md) for the project's own frontend architecture choices, which
are the binding conventions for this codebase — where this guide and AGENTS.md conflict, **AGENTS.md wins**.
