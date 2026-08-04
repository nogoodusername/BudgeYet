package com.famex.feature.auth.presentation

import com.famex.core.navigation.AuthTab

data class AuthUiState(
    val tab: AuthTab = AuthTab.LOG_IN,

    val loginEmail: String = "",
    val loginPin: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,

    // No PIN field here on purpose — the backend always generates and emails the PIN at
    // signup (see AuthRepository.signUp), so there's nothing for the user to type here.
    val signUpFullName: String = "",
    val signUpNickname: String = "",
    val signUpEmail: String = "",
    val isSigningUp: Boolean = false,
    val signUpError: String? = null
)
