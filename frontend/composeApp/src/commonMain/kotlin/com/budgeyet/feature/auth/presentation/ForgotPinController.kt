package com.budgeyet.feature.auth.presentation

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

sealed class ForgotPinEvent {
    data class Submitted(val email: String) : ForgotPinEvent()
}

class ForgotPinController(
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(ForgotPinUiState())
    val uiState: StateFlow<ForgotPinUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ForgotPinEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<ForgotPinEvent> = _events.asSharedFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email address") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            try {
                val email = state.email.trim()
                repository.requestPinReset(email)
                _uiState.update { it.copy(isSubmitting = false) }
                _events.emit(ForgotPinEvent.Submitted(email))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSubmitting = false, error = t.message ?: "Couldn't send a new PIN") }
            }
        }
    }
}
