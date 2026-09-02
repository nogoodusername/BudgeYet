# Changelog

## [Unreleased]
### Added
- Tapping a transaction on the Category Detail page now opens the Transaction Detail screen

### Fixed
- Sign the user out automatically when the access token is rejected (expired/invalid) instead of stranding them on an error screen with only a Retry button — the app now returns to onboarding so they can sign in again
- Keyboard no longer covers form fields and buttons with no way to dismiss it, most visibly on Category Limits — tapping any empty area now closes the keyboard, and scrollable screens make room for it (iOS especially, which has no hardware Back button)
- Bottom navigation bar no longer sits under the system navigation bar (Android gesture pill / 3-button bar) or the iOS home indicator, which had made tapping the tabs unreliable

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