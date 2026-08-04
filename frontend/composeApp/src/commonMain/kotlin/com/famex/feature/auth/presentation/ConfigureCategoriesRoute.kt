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

@Composable
fun ConfigureCategoriesRoute(
    household: Household,
    monthlyGoalAmount: Double,
    onFinished: (Household) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, household.id) {
        ConfigureCategoriesController(household, monthlyGoalAmount, container.authRepository, scope)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is ConfigureCategoriesEvent.Finished -> onFinished(event.household)
            }
        }
    }

    ConfigureCategoriesScreen(
        uiState = uiState,
        onToggleCategory = controller::onToggleCategory,
        onLimitChange = controller::onLimitChange,
        onCustomNameChange = controller::onCustomNameChange,
        onAutoDistributeToggle = controller::onAutoDistributeToggle,
        onAddCustomCategory = controller::onAddCustomCategory,
        onFinish = controller::onFinish,
        modifier = modifier
    )
}
