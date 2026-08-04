package com.famex.feature.transaction.presentation

import com.famex.core.model.Transaction

data class HistoryUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)
