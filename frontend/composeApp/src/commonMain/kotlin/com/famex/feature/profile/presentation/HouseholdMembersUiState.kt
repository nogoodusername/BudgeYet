package com.famex.feature.profile.presentation

import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole

data class RoleChangeRequest(val member: HouseholdMember, val newRole: MemberRole)

data class HouseholdMembersUiState(
    val isLoading: Boolean = false,
    val household: Household? = null,
    // The signed-in member's role, derived from household.members after load — gates the
    // member-management UI (role changes, remove, invite, revoke) for Admin/Owner only. null
    // until the household is loaded; treat as "no admin privileges".
    val currentUserRole: MemberRole? = null,
    val errorMessage: String? = null,
    val pendingRoleChange: RoleChangeRequest? = null,
    val pendingRemoveMember: HouseholdMember? = null,
    val isProcessing: Boolean = false,
    val actionError: String? = null,
    // Which pending invite a Revoke tap is in flight for, so only that row shows busy.
    val processingInviteId: Long? = null,
    // Which pending invite the last Revoke failure belongs to, so only that row shows the error.
    val failedInviteId: Long? = null,
    val inviteActionError: String? = null
)
