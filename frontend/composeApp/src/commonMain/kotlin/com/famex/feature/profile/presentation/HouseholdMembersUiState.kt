package com.famex.feature.profile.presentation

import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole

data class RoleChangeRequest(val member: HouseholdMember, val newRole: MemberRole)

data class HouseholdMembersUiState(
    val isLoading: Boolean = false,
    val household: Household? = null,
    val errorMessage: String? = null,
    val pendingRoleChange: RoleChangeRequest? = null,
    val pendingRemoveMember: HouseholdMember? = null,
    val isProcessing: Boolean = false,
    val actionError: String? = null
)
