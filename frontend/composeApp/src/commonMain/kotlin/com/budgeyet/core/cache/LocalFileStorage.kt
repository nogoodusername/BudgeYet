package com.budgeyet.core.cache

// File-backed string storage for the offline cache + write queue (core/cache + core/offline).
//
// Deliberately separate from SettingsStorage (SharedPreferences/NSUserDefaults): the offline
// layer persists larger JSON blobs — full transaction/category lists, the dashboard snapshot,
// the pending-write queue — that don't belong in preference stores. Files land in each
// platform's app-private cache directory (Android filesDir, iOS Library/Caches) and are never
// backed up / synced to the cloud.
//
// No TTL/eviction in v1 — it's a mirror of the last successful fetch (see LocalCacheStore).
interface LocalFileStorage {
    suspend fun readString(key: String): String?
    suspend fun writeString(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
}

expect fun createLocalFileStorage(): LocalFileStorage
