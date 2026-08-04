package com.famex.feature.auth.presentation

data class PinSentUiState(
    val email: String,
    val isResending: Boolean = false,
    val resendError: String? = null,
    val justResent: Boolean = false
)
