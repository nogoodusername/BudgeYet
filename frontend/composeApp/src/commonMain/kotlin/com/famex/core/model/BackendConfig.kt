package com.famex.core.model

import kotlinx.serialization.Serializable

// Device-level setting (PRD A0/Section 9.9), not per-household — default to the hosted backend,
// but let the user point the app at a self-hosted deployment instead. Persisted via
// AuthRepository.getBackendConfig/setBackendConfig (backed by SettingsStorage — see
// FakeAuthRepository), so a custom URL now survives a process restart.
@Serializable
sealed interface BackendConfig {
    @Serializable
    data object Hosted : BackendConfig

    @Serializable
    data class Custom(val url: String) : BackendConfig
}
