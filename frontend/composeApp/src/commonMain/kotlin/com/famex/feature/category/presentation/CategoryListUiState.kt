package com.famex.feature.category.presentation

import com.famex.core.model.Category
import kotlin.math.roundToInt

data class CategoryListUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    // categoryId -> raw text field input, so typing "6" then "0" isn't clobbered by
    // reformatting mid-edit; parsed to Double only when computing totals/saving.
    val limitDrafts: Map<Long, String> = emptyMap(),
    // Snapshot of the allocated total at the last load/save — the fixed reference point
    // "Allocated"/"Remaining" are measured against as the user reallocates.
    val totalMonthlyBudget: Double = 0.0,
    val currency: String = "USD",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val errorMessage: String? = null
) {
    val allocatedAmount: Double get() = limitDrafts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
    val remainingAmount: Double get() = totalMonthlyBudget - allocatedAmount

    fun draftFraction(category: Category): Float {
        val draft = limitDrafts[category.id]?.toDoubleOrNull() ?: category.monthlyLimit
        return if (totalMonthlyBudget > 0) (draft / totalMonthlyBudget).toFloat() else 0f
    }

    fun draftPercent(category: Category): Int = (draftFraction(category) * 100).roundToInt()
}
