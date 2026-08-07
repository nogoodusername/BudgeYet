package com.budgeyet.feature.transaction.presentation

import com.budgeyet.core.model.Category
import com.budgeyet.core.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = false,
    val transaction: Transaction? = null,
    val category: Category? = null,
    val currency: String = "USD",
    val isDeleting: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val errorMessage: String? = null
)
