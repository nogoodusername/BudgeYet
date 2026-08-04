package com.famex.core.persistence

import platform.Foundation.NSUserDefaults

// NSUserDefaults calls are cheap, synchronous, and thread-safe — no dispatcher hop needed,
// unlike the Android actual's SharedPreferences.edit().apply() (also technically async/fire-
// and-forget already, but wrapped in Dispatchers.IO there to keep the call off the main thread).
private class IosSettingsStorage(private val defaults: NSUserDefaults) : SettingsStorage {
    override suspend fun getString(key: String): String? = defaults.stringForKey(key)

    override suspend fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override suspend fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

actual fun createSettingsStorage(): SettingsStorage = IosSettingsStorage(NSUserDefaults.standardUserDefaults)
