# Changelog

## [Unreleased]
### Fixed
- Sign the user out automatically when the access token is rejected (expired/invalid) instead of stranding them on an error screen with only a Retry button — the app now returns to onboarding so they can sign in again
- Fix on-screen keyboard overlapping text fields and CTAs on iOS (remove `.ignoresSafeArea(.keyboard)`) and Android (`windowSoftInputMode=adjustResize`); add tap-outside-to-dismiss and an IME "Done" action on numeric/money fields so the keyboard can always be collapsed

## [1.1.1] - 2026-08-19
### Changed
- Enable R8 minification; rename dashboard title to Dashboard
- Upgrade Android Gradle Plugin to 8.11.2 and Gradle to 8.13 (full compileSdk 36 support)

## [1.1.0] - 2026-08-18
### Changed
- Target Android 16 (API 36); add unified cross-platform versioning and changelog

## [1.0.1] - 2026-08-18
### Changed
- Play Store release: bumped appId to com.imhx.budgeyet, versionCode 2 (Android only — iOS/web unaffected until next unified release)

## [1.0.0] - 2026-07-27
### Added
- Initial release