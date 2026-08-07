package com.budgeyet.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.budgeyet.core.di.LocalAppContainer
import com.budgeyet.core.model.Household
import com.budgeyet.core.util.currencySymbolFor

@Composable
fun BudgetGoalRoute(
    household: Household,
    onSaved: (Household, Double) -> Unit,
    onSkipped: (Household) -> Unit,
    // True when reached from the onboarding funnel (Next leads into Configure Categories, and
    // Skip is a valid way to finish onboarding without a budget). False when reused from the
    // main app (e.g. Dashboard's "Set Up Budget" for a household that already has categories) —
    // there, "skip" and "configure categories next" don't make sense, so BudgetGoalScreen swaps
    // in a plain Save action and hides Skip.
    isOnboarding: Boolean = true,
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
        isOnboarding = isOnboarding,
        onBudgetNameChange = controller::onBudgetNameChange,
        onGoalAmountChange = controller::onGoalAmountChange,
        onSave = controller::onSave,
        onSkip = controller::onSkip,
        modifier = modifier
    )
}
