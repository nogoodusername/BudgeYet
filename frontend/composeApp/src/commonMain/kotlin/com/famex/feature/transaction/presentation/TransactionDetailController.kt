package com.famex.feature.transaction.presentation

import com.famex.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TransactionDetailController(
    private val transactionId: Long,
    private val repository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val transaction = repository.getTransaction(transactionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    transaction = transaction,
                    errorMessage = if (transaction == null) "Transaction not found" else null
                )
            }
        }
    }
}
