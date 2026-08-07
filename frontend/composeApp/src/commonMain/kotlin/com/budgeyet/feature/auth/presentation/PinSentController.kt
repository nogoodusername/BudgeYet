package com.budgeyet.feature.auth.presentation

import com.budgeyet.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PinSentController(
    email: String,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(PinSentUiState(email = email))
    val uiState: StateFlow<PinSentUiState> = _uiState.asStateFlow()

    fun onResend() {
        val state = _uiState.value
        scope.launch {
            _uiState.update { it.copy(isResending = true, resendError = null, justResent = false) }
            try {
                repository.requestPinReset(state.email)
                _uiState.update { it.copy(isResending = false, justResent = true) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isResending = false, resendError = t.message ?: "Couldn't resend the PIN") }
            }
        }
    }
}
