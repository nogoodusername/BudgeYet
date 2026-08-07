package com.budgeyet.feature.category.presentation

import com.budgeyet.feature.category.domain.CategoryRepository
import com.budgeyet.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class CategoryListController(
    private val repository: CategoryRepository,
    private val profileRepository: ProfileRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val categories = repository.getCategories()
                val household = profileRepository.getHousehold()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        limitDrafts = categories.associate { c -> c.id to formatDraft(c.monthlyLimit) },
                        totalMonthlyBudget = categories.sumOf { c -> c.monthlyLimit },
                        currency = household.currency
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onLimitChange(categoryId: Long, rawValue: String) {
        _uiState.update { it.copy(limitDrafts = it.limitDrafts + (categoryId to rawValue)) }
    }

    fun onSplitEvenly() {
        _uiState.update { state ->
            if (state.categories.isEmpty()) return@update state
            val share = state.totalMonthlyBudget / state.categories.size
            state.copy(limitDrafts = state.categories.associate { it.id to formatDraft(share) })
        }
    }

    fun onSaveChanges() {
        scope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val updates = _uiState.value.limitDrafts.mapValues { (_, raw) -> raw.toDoubleOrNull() ?: 0.0 }
                repository.updateCategoryLimits(updates)
                val categories = repository.getCategories()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        categories = categories,
                        limitDrafts = categories.associate { c -> c.id to formatDraft(c.monthlyLimit) },
                        totalMonthlyBudget = categories.sumOf { c -> c.monthlyLimit }
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isSaving = false, saveError = t.message ?: "Couldn't save changes") }
            }
        }
    }
}

private fun formatDraft(amount: Double): String = amount.roundToInt().toString()
