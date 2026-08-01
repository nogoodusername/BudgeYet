package com.famex.feature.profile.presentation

import com.famex.core.model.HouseholdMember
import com.famex.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HouseholdMembersController(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(HouseholdMembersUiState())
    val uiState: StateFlow<HouseholdMembersUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val household = repository.getHousehold()
                _uiState.update { it.copy(isLoading = false, household = household) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onRequestPromote(member: HouseholdMember) = _uiState.update { it.copy(pendingPromoteMember = member, actionError = null) }

    fun onCancelPromote() = _uiState.update { it.copy(pendingPromoteMember = null) }

    fun onConfirmPromote() {
        val member = _uiState.value.pendingPromoteMember ?: return
        scope.launch {
            _uiState.update { it.copy(isProcessing = true, actionError = null) }
            try {
                val household = repository.promoteToAdmin(member.id)
                _uiState.update { it.copy(isProcessing = false, household = household, pendingPromoteMember = null) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isProcessing = false, actionError = t.message ?: "Couldn't promote member") }
            }
        }
    }

    fun onRequestRemove(member: HouseholdMember) = _uiState.update { it.copy(pendingRemoveMember = member, actionError = null) }

    fun onCancelRemove() = _uiState.update { it.copy(pendingRemoveMember = null) }

    fun onConfirmRemove() {
        val member = _uiState.value.pendingRemoveMember ?: return
        scope.launch {
            _uiState.update { it.copy(isProcessing = true, actionError = null) }
            try {
                val household = repository.removeMember(member.id)
                _uiState.update { it.copy(isProcessing = false, household = household, pendingRemoveMember = null) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isProcessing = false, actionError = t.message ?: "Couldn't remove member") }
            }
        }
    }
}
