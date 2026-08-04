package com.famex.feature.category.presentation

import com.famex.core.model.Category

data class CategoryListUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val errorMessage: String? = null
)
