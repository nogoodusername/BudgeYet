package com.famex.feature.auth.presentation

import com.famex.core.navigation.PinSentContext

data class PinSentUiState(
    val email: String,
    val context: PinSentContext,
    val isResending: Boolean = false,
    val resendError: String? = null,
    val justResent: Boolean = false
)
