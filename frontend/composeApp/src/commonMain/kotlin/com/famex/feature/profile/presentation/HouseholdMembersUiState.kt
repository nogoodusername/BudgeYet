package com.famex.feature.profile.presentation

import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember

data class HouseholdMembersUiState(
    val isLoading: Boolean = false,
    val household: Household? = null,
    val errorMessage: String? = null,
    val pendingPromoteMember: HouseholdMember? = null,
    val pendingRemoveMember: HouseholdMember? = null,
    val isProcessing: Boolean = false,
    val actionError: String? = null
)
