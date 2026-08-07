package com.budgeyet.feature.auth.presentation

data class ForgotPinUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
)
