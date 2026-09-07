package com.budgeyet.feature.category.presentation

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.core.model.Category
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
    private val cacheStore: LocalCacheStore,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    // See DashboardController — load-once guard so a tab switch doesn't refetch.
    private var hasLoaded = false

    fun load(forceRefresh: Boolean = false) {
        if (hasLoaded && !forceRefresh) return
        hasLoaded = true
        scope.launch {
            // Cache-first paint so revisiting Category Limits renders instantly.
            if (_uiState.value.categories.isEmpty()) {
                val cachedCategories = cacheStore.getCachedCategories()
                if (cachedCategories != null) {
                    val cachedCurrency = cacheStore.getCachedHousehold()?.currency
                    _uiState.update {
                        applyCategories(it, cachedCategories).let { s ->
                            if (cachedCurrency != null) s.copy(currency = cachedCurrency) else s
                        }
                    }
                }
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val categories = repository.getCategories()
                val household = profileRepository.getHousehold()
                _uiState.update {
                    applyCategories(it, categories).copy(isLoading = false, currency = household.currency)
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    private fun applyCategories(state: CategoryListUiState, categories: List<Category>): CategoryListUiState =
        state.copy(
            categories = categories,
            limitDrafts = categories.associate { c -> c.id to formatDraft(c.monthlyLimit) },
            totalMonthlyBudget = categories.sumOf { c -> c.monthlyLimit }
        )

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
