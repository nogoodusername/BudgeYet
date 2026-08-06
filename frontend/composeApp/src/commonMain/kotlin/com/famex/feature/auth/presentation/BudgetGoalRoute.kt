package com.famex.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer
import com.famex.core.model.Household
import com.famex.core.ui.MonthYearPickerDialog
import com.famex.core.util.currencySymbolFor

@Composable
fun BudgetGoalRoute(
    household: Household,
    onSaved: (Household, Double) -> Unit,
    onSkipped: (Household) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, household.id) { BudgetGoalController(household, container.authRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is BudgetGoalEvent.Saved -> onSaved(event.household, event.monthlyGoalAmount)
                is BudgetGoalEvent.Skipped -> onSkipped(event.household)
            }
        }
    }

    BudgetGoalScreen(
        uiState = uiState,
        currencySymbol = currencySymbolFor(household.currency),
        onBudgetNameChange = controller::onBudgetNameChange,
        onOpenPeriodPicker = controller::onOpenPeriodPicker,
        onGoalAmountChange = controller::onGoalAmountChange,
        onSave = controller::onSave,
        onSkip = controller::onSkip,
        modifier = modifier
    )

    if (uiState.showPeriodPicker) {
        MonthYearPickerDialog(
            selectedMonth = uiState.periodMonth,
            selectedYear = uiState.periodYear,
            onDismiss = controller::onClosePeriodPicker,
            onConfirm = controller::onPeriodSelected
        )
    }
}
