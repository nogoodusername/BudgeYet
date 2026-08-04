package com.famex.feature.category.presentation

import com.famex.feature.category.domain.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryDetailController(
    private val categoryId: Long,
    private val repository: CategoryRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val category = repository.getCategory(categoryId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    category = category,
                    errorMessage = if (category == null) "Category not found" else null
                )
            }
        }
    }
}
