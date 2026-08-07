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

@Composable
fun JoinHouseholdRoute(
    email: String,
    onJoined: (Household) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, email) { JoinHouseholdController(email, container.authRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is JoinHouseholdEvent.Joined -> onJoined(event.household)
            }
        }
    }

    JoinHouseholdScreen(
        uiState = uiState,
        onInviteCodeChange = controller::onInviteCodeChange,
        onJoin = controller::onJoin,
        modifier = modifier
    )
}
