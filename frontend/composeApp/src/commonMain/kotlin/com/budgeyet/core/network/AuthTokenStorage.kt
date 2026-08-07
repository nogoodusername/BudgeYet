package com.budgeyet.core.network

import com.budgeyet.core.persistence.SettingsStorage

// Access-token-only storage. The backend's Token schema (backend/app/schemas/user.py) returns
// just `access_token`/`token_type` — no refresh token — so there's no silent-refresh flow to
// implement: once the token expires, the user has to sign in again. Kept separate from
// core/model/AuthSession (which persists user/household for restoring the UI on cold start, not
// credentials), but backed by the same SettingsStorage — SharedPreferences/NSUserDefaults, not
// Keychain/EncryptedSharedPreferences, a limitation that already existed for AuthSession and
// isn't introduced here.
class AuthTokenStorage(private val settingsStorage: SettingsStorage) {
    suspend fun getToken(): String? = settingsStorage.getString(KEY_ACCESS_TOKEN)

    suspend fun setToken(token: String) {
        settingsStorage.putString(KEY_ACCESS_TOKEN, token)
    }

    suspend fun clearToken() {
        settingsStorage.remove(KEY_ACCESS_TOKEN)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "auth_access_token"
    }
}
