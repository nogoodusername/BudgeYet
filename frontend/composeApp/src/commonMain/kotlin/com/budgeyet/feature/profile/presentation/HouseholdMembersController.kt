package com.budgeyet.feature.profile.presentation

import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.PendingInvite
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

sealed class HouseholdMembersEvent {
    // The signed-in Owner deleted their (sole-member) household — App.kt drops the household
    // from the session so the user lands on the Create / Join Household chooser.
    data object HouseholdDeleted : HouseholdMembersEvent()
}

class HouseholdMembersController(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
    private val currentUserId: Long?
) {
    private val _uiState = MutableStateFlow(HouseholdMembersUiState())
    val uiState: StateFlow<HouseholdMembersUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HouseholdMembersEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<HouseholdMembersEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val household = repository.getHousehold()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        household = household,
                        currentUserRole = household.currentMemberRole(currentUserId),
                        currentUserId = currentUserId
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onRequestRoleChange(member: HouseholdMember, newRole: MemberRole) =
        _uiState.update { it.copy(pendingRoleChange = RoleChangeRequest(member, newRole), actionError = null) }

    fun onCancelRoleChange() = _uiState.update { it.copy(pendingRoleChange = null) }

    fun onConfirmRoleChange() {
        val request = _uiState.value.pendingRoleChange ?: return
        scope.launch {
            _uiState.update { it.copy(isProcessing = true, actionError = null) }
            try {
                val household = repository.updateMemberRole(request.member.id, request.newRole)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        household = household,
                        currentUserRole = household.currentMemberRole(currentUserId),
                        currentUserId = currentUserId,
                        pendingRoleChange = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isProcessing = false, actionError = t.message ?: "Couldn't update role") }
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
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        household = household,
                        currentUserRole = household.currentMemberRole(currentUserId),
                        currentUserId = currentUserId,
                        pendingRemoveMember = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isProcessing = false, actionError = t.message ?: "Couldn't remove member") }
            }
        }
    }

    fun onRequestDeleteHousehold() = _uiState.update { it.copy(pendingDeleteHousehold = true, actionError = null) }

    fun onCancelDeleteHousehold() = _uiState.update { it.copy(pendingDeleteHousehold = false) }

    fun onConfirmDeleteHousehold() {
        if (!_uiState.value.pendingDeleteHousehold) return
        scope.launch {
            _uiState.update { it.copy(isProcessing = true, actionError = null) }
            try {
                repository.deleteHousehold()
                _uiState.update { it.copy(isProcessing = false, pendingDeleteHousehold = false) }
                _events.emit(HouseholdMembersEvent.HouseholdDeleted)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isProcessing = false, actionError = t.message ?: "Couldn't delete household") }
            }
        }
    }

    fun onRevokeInvite(invite: PendingInvite) {
        scope.launch {
            _uiState.update { it.copy(processingInviteId = invite.id, failedInviteId = null, inviteActionError = null) }
            try {
                val household = repository.revokeInvite(invite.id)
                _uiState.update {
                    it.copy(
                        processingInviteId = null,
                        household = household,
                        currentUserRole = household.currentMemberRole(currentUserId),
                        currentUserId = currentUserId
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(processingInviteId = null, failedInviteId = invite.id, inviteActionError = t.message ?: "Couldn't revoke invite")
                }
            }
        }
    }
}
