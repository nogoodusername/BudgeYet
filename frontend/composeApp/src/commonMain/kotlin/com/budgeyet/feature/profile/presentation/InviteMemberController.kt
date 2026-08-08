package com.budgeyet.feature.profile.presentation

import com.budgeyet.core.model.Household
import com.budgeyet.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class InviteMemberEvent {
    data object Invited : InviteMemberEvent()
}

private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

class InviteMemberController(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(InviteMemberUiState())
    val uiState: StateFlow<InviteMemberUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InviteMemberEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<InviteMemberEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val household = repository.ensureJoinCode()
                _uiState.update { it.copy(isLoading = false, household = household) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onEmailChange(value: String) = _uiState.update { it.copy(emailDraft = value, sendError = null) }

    fun onSendInvite() {
        val state = _uiState.value
        val household = state.household ?: return
        if (household.members.size >= Household.MAX_MEMBERS) {
            _uiState.update { it.copy(sendError = "Household is full") }
            return
        }
        if (!emailRegex.matches(state.emailDraft.trim())) {
            _uiState.update { it.copy(sendError = "Enter a valid email address") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSending = true, sendError = null) }
            try {
                repository.inviteMember(state.emailDraft.trim())
                _uiState.update { it.copy(isSending = false, emailDraft = "") }
                _events.emit(InviteMemberEvent.Invited)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSending = false, sendError = t.message ?: "Couldn't send invite") }
            }
        }
    }
}
