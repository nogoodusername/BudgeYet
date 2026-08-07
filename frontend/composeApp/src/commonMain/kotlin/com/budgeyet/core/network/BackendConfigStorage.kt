package com.budgeyet.core.network

import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.persistence.SettingsStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Shared BackendConfig persistence — every Real*Repository that builds a request URL needs this
// (via BackendConfig.apiUrl, see ApiEndpoint.kt), not just AuthRepository, which is why it moved
// out of RealAuthRepository into core/network alongside the rest of the shared client plumbing.
class BackendConfigStorage(private val settingsStorage: SettingsStorage) {
    suspend fun get(): BackendConfig {
        val json = settingsStorage.getString(KEY_BACKEND_CONFIG) ?: return BackendConfig.Hosted
        return try {
            appJson.decodeFromString<BackendConfig>(json)
        } catch (e: Exception) {
            BackendConfig.Hosted
        }
    }

    suspend fun set(config: BackendConfig) {
        settingsStorage.putString(KEY_BACKEND_CONFIG, appJson.encodeToString(config))
    }

    private companion object {
        const val KEY_BACKEND_CONFIG = "backend_config"
    }
}

// ignoreUnknownKeys so an older persisted blob doesn't crash decoding after BackendConfig gains
// a field.
private val appJson = Json { ignoreUnknownKeys = true }
