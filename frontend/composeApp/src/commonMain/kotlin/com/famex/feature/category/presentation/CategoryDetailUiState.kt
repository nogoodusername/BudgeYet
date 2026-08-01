package com.famex.feature.category.presentation

import com.famex.core.model.Category
import com.famex.core.model.Transaction

data class CategoryDetailUiState(
    val isLoading: Boolean = false,
    val category: Category? = null,
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null
)
