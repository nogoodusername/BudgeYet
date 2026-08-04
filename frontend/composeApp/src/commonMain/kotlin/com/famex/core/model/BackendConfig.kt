package com.famex.core.model

// Device-level setting (PRD A0/Section 9.9), not per-household — default to the hosted backend,
// but let the user point the app at a self-hosted deployment instead. In-memory only for now:
// there's no local persistence layer (DataStore etc.) yet and no real networking to validate a
// custom URL against, so this doesn't survive a process restart until Phase 2's networking lands.
sealed interface BackendConfig {
    data object Hosted : BackendConfig
    data class Custom(val url: String) : BackendConfig
}
