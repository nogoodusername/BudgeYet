package com.famex.feature.profile.presentation

import com.famex.core.model.Household
import com.famex.core.model.MemberRole
import com.famex.core.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val household: Household? = null,
    // The signed-in member's role, derived from household.members after load — gates the
    // admin-only Household Settings card (currency/language, Manage Members). null until the
    // household is loaded; treat as "no admin privileges".
    val currentUserRole: MemberRole? = null,
    val fullNameDraft: String = "",
    val nicknameDraft: String = "",
    val isSavingProfile: Boolean = false,
    val saveError: String? = null,
    val errorMessage: String? = null,
    val showSignOutDialog: Boolean = false
) {
    val hasUnsavedNameChanges: Boolean
        get() = user != null && (fullNameDraft != user.fullName || nicknameDraft != user.nickname)
}
