package com.budgeyet.feature.profile.presentation

import com.budgeyet.core.model.Household
import com.budgeyet.core.util.toDisplayText

data class InviteMemberUiState(
    val isLoading: Boolean = false,
    val household: Household? = null,
    val emailDraft: String = "",
    val isSending: Boolean = false,
    val sendError: String? = null,
    val errorMessage: String? = null
) {
    val joinCode: String get() = "BUDGE-YET-${(household?.id ?: 0L).toString().padStart(2, '0')}"
    val joinCodeExpiryText: String? get() = household?.joinCodeExpiresAt?.toDisplayText()
}
