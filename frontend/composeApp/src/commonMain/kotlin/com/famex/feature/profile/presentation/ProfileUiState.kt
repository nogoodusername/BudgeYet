package com.famex.feature.profile.presentation

import com.famex.core.model.DisplayMode
import com.famex.core.model.Household
import com.famex.core.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val household: Household? = null,
    val fullNameDraft: String = "",
    val nicknameDraft: String = "",
    val currencyDraft: String = "USD",
    val languageDraft: String = "en",
    val displayModeDraft: DisplayMode = DisplayMode.SYSTEM,
    val pushNotificationsDraft: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val errorMessage: String? = null
) {
    val hasUnsavedChanges: Boolean
        get() = user != null && household != null && (
            fullNameDraft != user.fullName ||
                nicknameDraft != user.nickname ||
                displayModeDraft != user.displayMode ||
                pushNotificationsDraft != user.pushNotificationsEnabled ||
                currencyDraft != household.currency ||
                languageDraft != household.language
            )
}
