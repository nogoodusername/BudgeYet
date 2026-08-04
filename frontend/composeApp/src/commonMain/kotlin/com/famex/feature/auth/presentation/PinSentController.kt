package com.famex.feature.auth.presentation

import com.famex.core.navigation.PinSentContext
import com.famex.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PinSentController(
    email: String,
    context: PinSentContext,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(PinSentUiState(email = email, context = context))
    val uiState: StateFlow<PinSentUiState> = _uiState.asStateFlow()

    // Same underlying call for both contexts — the backend's forgot_pin endpoint issues and
    // emails a fresh PIN "mirroring the signup flow" (see AuthService.forgot_pin), so resending
    // after a fresh signup is functionally identical to a PIN reset.
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
