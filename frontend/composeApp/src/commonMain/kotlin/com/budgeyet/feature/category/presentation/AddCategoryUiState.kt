package com.budgeyet.feature.category.presentation

import com.budgeyet.core.ui.categoryIconChoices

data class AddCategoryUiState(
    val name: String = "",
    val monthlyLimitText: String = "",
    val selectedIcon: String = categoryIconChoices.first(),
    val currency: String = "USD",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isIconPickerOpen: Boolean = false
) {
    val previewName: String get() = name.ifBlank { "New Category" }

    val previewLimit: Double get() = monthlyLimitText.toDoubleOrNull() ?: 0.0
}
