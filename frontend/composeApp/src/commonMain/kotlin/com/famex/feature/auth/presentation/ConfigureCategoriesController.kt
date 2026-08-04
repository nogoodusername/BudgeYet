package com.famex.feature.auth.presentation

import com.famex.core.model.Household
import com.famex.feature.auth.domain.AuthRepository
import com.famex.feature.auth.domain.CategorySetupInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ConfigureCategoriesEvent {
    data class Finished(val household: Household) : ConfigureCategoriesEvent()
}

class ConfigureCategoriesController(
    private val household: Household,
    monthlyGoalAmount: Double,
    private val repository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(ConfigureCategoriesUiState(monthlyGoalAmount = monthlyGoalAmount))
    val uiState: StateFlow<ConfigureCategoriesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ConfigureCategoriesEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<ConfigureCategoriesEvent> = _events.asSharedFlow()

    private var nextCustomId = 1

    fun onToggleCategory(key: String) {
        _uiState.update { state ->
            val updated = state.categories.map { if (it.key == key) it.copy(isSelected = !it.isSelected) else it }
            state.copy(categories = if (state.autoDistribute) applyAutoDistribute(updated, state.monthlyGoalAmount) else updated, error = null)
        }
    }

    fun onLimitChange(key: String, value: String) {
        if (value.isNotEmpty() && !value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) return
        _uiState.update { state ->
            state.copy(categories = state.categories.map { if (it.key == key) it.copy(monthlyLimitText = value) else it }, error = null)
        }
    }

    fun onCustomNameChange(key: String, value: String) {
        _uiState.update { state ->
            state.copy(categories = state.categories.map { if (it.key == key) it.copy(name = value) else it }, error = null)
        }
    }

    fun onAutoDistributeToggle() {
        _uiState.update { state ->
            val enabled = !state.autoDistribute
            state.copy(
                autoDistribute = enabled,
                categories = if (enabled) applyAutoDistribute(state.categories, state.monthlyGoalAmount) else state.categories
            )
        }
    }

    fun onAddCustomCategory() {
        _uiState.update { state ->
            val newCategory = ConfigureCategoryItem(
                key = "custom-${nextCustomId++}",
                name = "",
                description = "Custom category",
                icon = "category",
                isSelected = true,
                isCustom = true
            )
            state.copy(categories = state.categories + newCategory, error = null)
        }
    }

    fun onFinish() {
        val state = _uiState.value
        val selected = state.categories.filter { it.isSelected }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one category") }
            return
        }
        if (selected.any { it.isCustom && it.name.isBlank() }) {
            _uiState.update { it.copy(error = "Name your custom category") }
            return
        }
        val limits = selected.map { it.monthlyLimitText.toDoubleOrNull() }
        if (limits.any { it == null || it < 0.0 }) {
            _uiState.update { it.copy(error = "Enter a valid monthly limit for each selected category") }
            return
        }
        val inputs = selected.zip(limits).map { (category, limit) ->
            CategorySetupInput(name = category.name.trim(), icon = category.icon, monthlyLimit = limit!!)
        }
        scope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.setupCategories(household.id, inputs)
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(ConfigureCategoriesEvent.Finished(household))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, error = t.message ?: "Couldn't save categories") }
            }
        }
    }

    private fun applyAutoDistribute(categories: List<ConfigureCategoryItem>, monthlyGoalAmount: Double): List<ConfigureCategoryItem> {
        val selectedCount = categories.count { it.isSelected }
        if (selectedCount == 0) return categories
        val equalShare = monthlyGoalAmount / selectedCount
        val formattedShare = formatTwoDecimals(equalShare)
        return categories.map { if (it.isSelected) it.copy(monthlyLimitText = formattedShare) else it }
    }

    private fun formatTwoDecimals(value: Double): String {
        val cents = kotlin.math.round(value * 100).toLong()
        val wholePart = cents / 100
        val fractionPart = (cents % 100).let { if (it < 0) -it else it }
        return "$wholePart.${fractionPart.toString().padStart(2, '0')}"
    }
}
