package com.famex.feature.category.presentation

import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryDetailController(
    private val categoryId: Long,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val category = categoryRepository.getCategory(categoryId)
            val transactions = transactionRepository.getTransactions().filter { it.categoryId == categoryId }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    category = category,
                    transactions = transactions,
                    errorMessage = if (category == null) "Category not found" else null
                )
            }
        }
    }
}
