package com.budgeyet.feature.auth.presentation

import com.budgeyet.core.model.AuthSession
import com.budgeyet.core.navigation.AuthTab
import com.budgeyet.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AuthEvent {
    data class LoggedIn(val session: AuthSession) : AuthEvent()
}

class AuthController(
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun onTabChange(tab: AuthTab) = _uiState.update { it.copy(tab = tab) }

    fun onLoginEmailChange(value: String) = _uiState.update { it.copy(loginEmail = value, loginError = null) }
    fun onLoginPinChange(value: String) = _uiState.update { it.copy(loginPin = value, loginError = null) }

    fun onLogin() {
        val state = _uiState.value
        if (state.loginEmail.isBlank() || state.loginPin.length != 6) {
            _uiState.update { it.copy(loginError = "Enter your email and 6-digit PIN") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            try {
                val session = repository.login(state.loginEmail.trim(), state.loginPin)
                _uiState.update { it.copy(isLoggingIn = false) }
                _events.emit(AuthEvent.LoggedIn(session))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoggingIn = false, loginError = t.message ?: "Couldn't sign in") }
            }
        }
    }

    fun onSignUpFullNameChange(value: String) = _uiState.update { it.copy(signUpFullName = value, signUpError = null) }
    fun onSignUpNicknameChange(value: String) = _uiState.update { it.copy(signUpNickname = value, signUpError = null) }
    fun onSignUpEmailChange(value: String) = _uiState.update { it.copy(signUpEmail = value, signUpError = null) }
    fun onSignUpPinChange(value: String) = _uiState.update { it.copy(signUpPin = value, signUpError = null) }
    fun onSignUpPinConfirmChange(value: String) = _uiState.update { it.copy(signUpPinConfirm = value, signUpError = null) }

    fun onSignUp() {
        val state = _uiState.value
        val validationError = when {
            state.signUpFullName.isBlank() || state.signUpEmail.isBlank() -> "Enter your name and email"
            state.signUpPin.length != 6 -> "Choose a 6-digit PIN"
            state.signUpPin != state.signUpPinConfirm -> "PINs don't match"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(signUpError = validationError) }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSigningUp = true, signUpError = null) }
            try {
                val email = state.signUpEmail.trim()
                repository.signUp(state.signUpFullName.trim(), state.signUpNickname.trim(), email, state.signUpPin)
                // The user already knows the PIN they just chose, so log straight in rather
                // than sending them through a "check your email" detour that no longer applies.
                val session = repository.login(email, state.signUpPin)
                _uiState.update { it.copy(isSigningUp = false) }
                _events.emit(AuthEvent.LoggedIn(session))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSigningUp = false, signUpError = t.message ?: "Couldn't create account") }
            }
        }
    }
}
