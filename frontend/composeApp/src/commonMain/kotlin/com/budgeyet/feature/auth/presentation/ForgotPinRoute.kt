package com.budgeyet.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.budgeyet.core.di.LocalAppContainer

@Composable
fun ForgotPinRoute(
    onSubmitted: (email: String) -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) { ForgotPinController(container.authRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is ForgotPinEvent.Submitted -> onSubmitted(event.email)
            }
        }
    }

    ForgotPinScreen(
        uiState = uiState,
        onEmailChange = controller::onEmailChange,
        onSubmit = controller::onSubmit,
        onBackToSignIn = onBackToSignIn,
        modifier = modifier
    )
}
