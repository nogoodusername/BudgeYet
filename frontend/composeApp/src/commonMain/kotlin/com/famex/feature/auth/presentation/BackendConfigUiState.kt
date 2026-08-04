package com.famex.feature.auth.presentation

enum class BackendConfigSelection { HOSTED, CUSTOM }

// Mirrors the Stitch mockup's status-indicator states (INVALID shows its "Enter a valid URL"
// hint even before any network call). CHECKING/REACHABLE/UNREACHABLE come from a real ping
// against the target server rather than the mockup's simulated timeout.
enum class ReachabilityStatus { IDLE, INVALID, CHECKING, REACHABLE, UNREACHABLE }

data class BackendConfigUiState(
    val selection: BackendConfigSelection = BackendConfigSelection.HOSTED,
    val customUrl: String = "",
    val reachability: ReachabilityStatus = ReachabilityStatus.IDLE,
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    // Hosted is always trusted; a custom URL must have pinged successfully first — matches the
    // mockup's Save button staying disabled until the status indicator shows "Server Reachable".
    val canSave: Boolean
        get() = selection == BackendConfigSelection.HOSTED || reachability == ReachabilityStatus.REACHABLE
}
