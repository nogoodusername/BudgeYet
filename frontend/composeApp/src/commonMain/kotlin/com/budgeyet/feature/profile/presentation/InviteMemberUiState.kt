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
    // The real backend-issued token for the household's blank-email invite (see
    // ProfileRepository.ensureJoinCode) — null while that invite is still being fetched/created.
    val joinCode: String? get() = household?.pendingInvites?.firstOrNull { it.email.isBlank() }?.token
    val joinCodeExpiryText: String? get() = household?.joinCodeExpiresAt?.toDisplayText()
}
