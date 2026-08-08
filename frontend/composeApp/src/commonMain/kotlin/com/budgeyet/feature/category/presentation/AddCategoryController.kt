package com.budgeyet.feature.category.presentation

import com.budgeyet.feature.category.domain.CategoryRepository
import com.budgeyet.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AddCategoryEvent {
    data object Saved : AddCategoryEvent()
}

class AddCategoryController(
    private val repository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AddCategoryUiState())
    val uiState: StateFlow<AddCategoryUiState> = _uiState.asStateFlow()

    // Save is a one-time event, not state — matches the AddTransaction controller's pattern.
    private val _events = MutableSharedFlow<AddCategoryEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<AddCategoryEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            val household = profileRepository.getHousehold()
            _uiState.update { it.copy(currency = household.currency) }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, saveError = null) }

    fun onMonthlyLimitChange(rawValue: String) =
        _uiState.update { it.copy(monthlyLimitText = sanitizeAmountInput(rawValue), saveError = null) }

    fun onIconSelected(icon: String) = _uiState.update { it.copy(selectedIcon = icon, isIconPickerOpen = false) }

    fun onSeeAllIcons() = _uiState.update { it.copy(isIconPickerOpen = true) }

    fun onDismissIconPicker() = _uiState.update { it.copy(isIconPickerOpen = false) }

    fun onSave() {
        val state = _uiState.value
        val monthlyLimit = state.monthlyLimitText.toDoubleOrNull() ?: 0.0

        val validationError = when {
            state.name.isBlank() -> "Enter a category name"
            monthlyLimit < 0.0 -> "Enter a valid monthly limit"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(saveError = validationError) }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                repository.createCategory(
                    name = state.name.trim(),
                    icon = state.selectedIcon,
                    monthlyLimit = monthlyLimit
                )
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddCategoryEvent.Saved)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, saveError = t.message ?: "Couldn't add category") }
            }
        }
    }
}

// Keeps the limit field a plain decimal string (digits + a single '.'), matching the
// amount-field sanitizer in AddTransactionController.
private fun sanitizeAmountInput(rawValue: String): String {
    var seenDot = false
    return rawValue.filter { c ->
        when {
            c.isDigit() -> true
            c == '.' && !seenDot -> { seenDot = true; true }
            else -> false
        }
    }
}
