package com.famex.feature.auth.presentation

import com.famex.core.model.Household
import com.famex.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class JoinHouseholdEvent {
    data class Joined(val household: Household) : JoinHouseholdEvent()
}

class JoinHouseholdController(
    private val email: String,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(JoinHouseholdUiState())
    val uiState: StateFlow<JoinHouseholdUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<JoinHouseholdEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<JoinHouseholdEvent> = _events.asSharedFlow()

    fun onInviteCodeChange(value: String) = _uiState.update { it.copy(inviteCode = value, error = null) }

    fun onJoin() {
        val state = _uiState.value
        if (state.inviteCode.isBlank()) {
            _uiState.update { it.copy(error = "Enter an invite code") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            try {
                val household = repository.joinHousehold(email, state.inviteCode.trim())
                _uiState.update { it.copy(isJoining = false) }
                _events.emit(JoinHouseholdEvent.Joined(household))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isJoining = false, error = t.message ?: "Couldn't join household") }
            }
        }
    }
}
