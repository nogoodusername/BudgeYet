package com.famex.feature.auth.presentation

import com.famex.core.util.todayLocalDate

data class BudgetGoalUiState(
    val budgetName: String = "",
    val budgetPeriod: String = "",
    val periodMonth: Int = todayLocalDate().monthNumber,
    val periodYear: Int = todayLocalDate().year,
    val showPeriodPicker: Boolean = false,
    val monthlyGoalAmountText: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)
