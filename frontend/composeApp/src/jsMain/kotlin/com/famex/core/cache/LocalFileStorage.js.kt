package com.famex.core.cache

import kotlinx.browser.localStorage

private const val KEY_PREFIX = "famex_cache_"

private class JsLocalFileStorage : LocalFileStorage {
    override suspend fun readString(key: String): String? = localStorage.getItem(KEY_PREFIX + key)

    override suspend fun writeString(key: String, value: String) {
        localStorage.setItem(KEY_PREFIX + key, value)
    }

    override suspend fun remove(key: String) {
        localStorage.removeItem(KEY_PREFIX + key)
    }

    override suspend fun clear() {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until localStorage.length) {
            val k = localStorage.key(i)
            if (k != null && k.startsWith(KEY_PREFIX)) {
                keysToRemove.add(k)
            }
        }
        keysToRemove.forEach { localStorage.removeItem(it) }
    }
}

actual fun createLocalFileStorage(): LocalFileStorage = JsLocalFileStorage()