package com.famex.feature.transaction.presentation

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

sealed class TransactionDetailEvent {
    data object Deleted : TransactionDetailEvent()
}

class TransactionDetailController(
    private val transactionId: Long,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TransactionDetailEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<TransactionDetailEvent> = _events.asSharedFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transaction = transactionRepository.getTransaction(transactionId)
                val category = transaction?.categoryId?.let { id ->
                    categoryRepository.getCategories().find { it.id == id }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        transaction = transaction,
                        category = category,
                        errorMessage = if (transaction == null) "Transaction not found" else null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }

    fun onRequestDelete() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun onCancelDelete() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun onConfirmDelete() {
        scope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                transactionRepository.deleteTransaction(transactionId)
                _uiState.update { it.copy(isDeleting = false, showDeleteConfirm = false) }
                _events.emit(TransactionDetailEvent.Deleted)
            } catch (t: Throwable) {
                _uiState.update { it.copy(isDeleting = false, errorMessage = t.message ?: "Couldn't delete transaction") }
            }
        }
    }
}
