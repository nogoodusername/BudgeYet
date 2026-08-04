package com.famex.core.persistence

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Populated once from MainActivity.onCreate before setContent{} runs — the only place in the
// app allowed to touch a real Android Context, per "never pass Context through layers, inject
// at the platform module level only." Every other androidMain/commonMain caller goes through
// createSettingsStorage() below instead of reaching for this directly.
internal object AndroidAppContext {
    lateinit var applicationContext: Context
}

private const val PREFS_NAME = "famex_settings"

private class AndroidSettingsStorage(context: Context) : SettingsStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)
    }

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(key).apply()
    }
}

actual fun createSettingsStorage(): SettingsStorage = AndroidSettingsStorage(AndroidAppContext.applicationContext)
