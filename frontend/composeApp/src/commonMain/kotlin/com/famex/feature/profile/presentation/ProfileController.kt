package com.famex.feature.profile.presentation

import com.famex.core.model.DisplayMode
import com.famex.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileController(
    private val repository: ProfileRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val user = repository.getCurrentUser()
                val household = repository.getHousehold()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        household = household,
                        fullNameDraft = user.fullName,
                        nicknameDraft = user.nickname
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullNameDraft = value) }

    fun onNicknameChange(value: String) = _uiState.update { it.copy(nicknameDraft = value) }

    fun onSaveProfile() {
        val state = _uiState.value
        if (state.fullNameDraft.isBlank()) {
            _uiState.update { it.copy(saveError = "Enter your full name") }
            return
        }
        if (state.nicknameDraft.isBlank()) {
            _uiState.update { it.copy(saveError = "Enter a nickname") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSavingProfile = true, saveError = null) }
            try {
                val user = repository.updateProfileName(state.fullNameDraft.trim(), state.nicknameDraft.trim())
                _uiState.update { it.copy(isSavingProfile = false, user = user) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSavingProfile = false, saveError = t.message ?: "Couldn't save changes") }
            }
        }
    }

    // Everything below applies immediately on selection — no separate Save step.

    fun onCurrencyChange(value: String) {
        scope.launch {
            val household = repository.updateCurrency(value)
            _uiState.update { it.copy(household = household) }
        }
    }

    fun onLanguageChange(value: String) {
        scope.launch {
            val household = repository.updateLanguage(value)
            _uiState.update { it.copy(household = household) }
        }
    }

    fun onDisplayModeChange(mode: DisplayMode) {
        scope.launch {
            val user = repository.updateDisplayMode(mode)
            _uiState.update { it.copy(user = user) }
        }
    }

    fun onPushNotificationsToggle(enabled: Boolean) {
        scope.launch {
            val user = repository.updatePushNotifications(enabled)
            _uiState.update { it.copy(user = user) }
        }
    }

    // Actually clearing the session lives at the App root (see App.kt) — this controller only
    // owns the confirmation dialog's visibility, same split as CategoryDetailController's
    // showDeleteDialog.
    fun onRequestSignOut() = _uiState.update { it.copy(showSignOutDialog = true) }

    fun onCancelSignOut() = _uiState.update { it.copy(showSignOutDialog = false) }
}
