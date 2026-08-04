package com.famex.feature.auth.presentation

data class BudgetGoalUiState(
    val budgetName: String = "",
    val budgetPeriod: String = "",
    val monthlyGoalAmountText: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)
