package com.famex.feature.transaction.presentation

import com.famex.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryController(
    private val repository: TransactionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transactions = repository.getTransactions()
                _uiState.update { it.copy(isLoading = false, transactions = transactions) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isLoading = false, errorMessage = t.message ?: "Something went wrong") }
            }
        }
    }
}
