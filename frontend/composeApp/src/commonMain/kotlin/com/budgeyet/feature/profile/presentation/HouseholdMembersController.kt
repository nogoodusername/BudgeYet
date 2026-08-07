package com.budgeyet.feature.profile.presentation

import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.PendingInvite
import com.budgeyet.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HouseholdMembersController(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope,
    private val currentUserId: Long?
) {
    private val _uiState = MutableStateFlow(HouseholdMembersUiState())
    val uiState: StateFlow<HouseholdMembersUiState> = _uiState.asStateFlow()

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
