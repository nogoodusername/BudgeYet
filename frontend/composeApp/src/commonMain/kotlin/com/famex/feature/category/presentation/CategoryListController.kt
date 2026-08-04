package com.famex.feature.category.presentation

import com.famex.feature.category.domain.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryListController(
    private val repository: CategoryRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryListUiState())
    val uiState: StateFlow<CategoryListUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val categories = repository.getCategories()
                _uiState.update { it.copy(isLoading = false, categories = categories) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }
}
