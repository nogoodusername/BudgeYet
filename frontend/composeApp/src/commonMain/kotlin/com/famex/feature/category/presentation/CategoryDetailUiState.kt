package com.famex.feature.category.presentation

import com.famex.core.model.Category

data class CategoryDetailUiState(
    val isLoading: Boolean = false,
    val category: Category? = null,
    val errorMessage: String? = null
)
