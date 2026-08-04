package com.famex.feature.category.presentation

import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class CategoryDetailEvent {
    data object Deleted : CategoryDetailEvent()
}

class CategoryDetailController(
    private val categoryId: Long,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CategoryDetailUiState())
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CategoryDetailEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<CategoryDetailEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val category = categoryRepository.getCategory(categoryId)
            val allCategories = categoryRepository.getCategories()
            val transactions = transactionRepository.getTransactions().filter { it.categoryId == categoryId }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    category = category,
                    transactions = transactions,
                    otherCategories = allCategories.filter { c -> c.id != categoryId },
                    errorMessage = if (category == null) "Category not found" else null
                )
            }
        }
    }

    fun onRequestDelete() = _uiState.update { it.copy(showDeleteDialog = true, deleteError = null, reassignToCategoryId = null) }

    fun onCancelDelete() = _uiState.update { it.copy(showDeleteDialog = false, deleteError = null, reassignToCategoryId = null) }

    fun onReassignTargetSelected(targetCategoryId: Long) =
        _uiState.update { it.copy(reassignToCategoryId = targetCategoryId) }

    fun onConfirmDelete() {
        val state = _uiState.value
        if (state.transactions.isNotEmpty() && state.reassignToCategoryId == null) {
            _uiState.update { it.copy(deleteError = "Choose a category to reassign these transactions to.") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            try {
                val targetId = state.reassignToCategoryId
                if (state.transactions.isNotEmpty() && targetId != null) {
                    val targetName = state.otherCategories.find { it.id == targetId }?.name.orEmpty()
                    transactionRepository.reassignCategory(categoryId, targetId, targetName)
                }
                categoryRepository.deleteCategory(categoryId)
                _uiState.update { it.copy(isDeleting = false, showDeleteDialog = false) }
                _events.emit(CategoryDetailEvent.Deleted)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isDeleting = false, deleteError = t.message ?: "Couldn't delete category") }
            }
        }
    }
}
