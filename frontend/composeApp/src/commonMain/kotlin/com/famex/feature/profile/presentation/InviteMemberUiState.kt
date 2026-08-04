package com.famex.feature.profile.presentation

import com.famex.core.model.Household

data class InviteMemberUiState(
    val isLoading: Boolean = false,
    val household: Household? = null,
    val emailDraft: String = "",
    val isSending: Boolean = false,
    val sendError: String? = null,
    val errorMessage: String? = null
) {
    val joinCode: String get() = "FAM-EX-${(household?.id ?: 0L).toString().padStart(2, '0')}"
}
