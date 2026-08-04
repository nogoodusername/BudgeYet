package com.famex.feature.auth.presentation

enum class BackendConfigSelection { HOSTED, CUSTOM }

data class BackendConfigUiState(
    val selection: BackendConfigSelection = BackendConfigSelection.HOSTED,
    val customUrl: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null
)
