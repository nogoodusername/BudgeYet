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
fun CreateHouseholdRoute(
    email: String,
    onCreated: (Household) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, email) { CreateHouseholdController(email, container.authRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is CreateHouseholdEvent.Created -> onCreated(event.household)
            }
        }
    }

    CreateHouseholdScreen(
        uiState = uiState,
        onNameChange = controller::onNameChange,
        onCurrencyChange = controller::onCurrencyChange,
        onCycleStartDayChange = controller::onCycleStartDayChange,
        onCreate = controller::onCreate,
        modifier = modifier
    )
}
