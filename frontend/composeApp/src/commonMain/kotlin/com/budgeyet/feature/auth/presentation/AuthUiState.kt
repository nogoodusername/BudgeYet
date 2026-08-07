package com.budgeyet.feature.auth.presentation

import com.budgeyet.core.navigation.AuthTab

data class AuthUiState(
    val tab: AuthTab = AuthTab.LOG_IN,

    val loginEmail: String = "",
    val loginPin: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,

    val signUpFullName: String = "",
    val signUpNickname: String = "",
    val signUpEmail: String = "",
    // User-chosen at signup (see AuthRepository.signUp) — confirm exists purely to catch typos
    // before it's hashed server-side; only signUpPin is ever sent.
    val signUpPin: String = "",
    val signUpPinConfirm: String = "",
    val isSigningUp: Boolean = false,
    val signUpError: String? = null
)
