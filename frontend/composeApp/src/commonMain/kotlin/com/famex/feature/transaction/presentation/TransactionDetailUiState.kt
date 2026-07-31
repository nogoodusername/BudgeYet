package com.famex.feature.transaction.presentation

import com.famex.core.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = false,
    val transaction: Transaction? = null,
    val errorMessage: String? = null
)
