package com.budgeyet.feature.category.presentation

import com.budgeyet.core.model.Category
import com.budgeyet.core.model.Transaction

data class CategoryDetailUiState(
    val isLoading: Boolean = false,
    val category: Category? = null,
    val currency: String = "USD",
    val transactions: List<Transaction> = emptyList(),
    val errorMessage: String? = null,
    // Other household categories, offered as reassign targets when deleting this one (PRD C1).
    val otherCategories: List<Category> = emptyList(),
    val showDeleteDialog: Boolean = false,
    val reassignToCategoryId: Long? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null
)
