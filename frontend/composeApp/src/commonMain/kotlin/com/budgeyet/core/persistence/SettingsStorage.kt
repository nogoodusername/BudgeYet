package com.budgeyet.core.persistence

// Minimal key-value persistence — deliberately not DataStore/Room (no KSP/annotation-processor
// dependency to add against this project's pinned Kotlin 1.9.23/Compose Multiplatform 1.6.10
// toolchain, same version-risk reasoning as avoiding Koin/navigation-compose; see AGENTS.md
// "Architecture choices made in Phase 1"). Backed by SharedPreferences on Android and
// NSUserDefaults on iOS — both first-party, zero extra dependencies, and more than sufficient
// for the handful of JSON blobs (AuthSession, BackendConfig) this app needs to survive a
// cold start.
interface SettingsStorage {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
}

expect fun createSettingsStorage(): SettingsStorage
