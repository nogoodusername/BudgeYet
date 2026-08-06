package com.famex.core.persistence

import kotlinx.browser.localStorage

private class JsSettingsStorage : SettingsStorage {
    override suspend fun getString(key: String): String? = localStorage.getItem(key)

    override suspend fun putString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    override suspend fun remove(key: String) {
        localStorage.removeItem(key)
    }
}

actual fun createSettingsStorage(): SettingsStorage = JsSettingsStorage()