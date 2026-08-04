package com.famex.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer
import com.famex.core.navigation.PinSentContext

@Composable
fun PinSentRoute(
    email: String,
    context: PinSentContext,
    onGoToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, email, context) {
        PinSentController(email, context, container.authRepository, scope)
    }
    val uiState by controller.uiState.collectAsState()

    PinSentScreen(
        uiState = uiState,
        onGoToSignIn = onGoToSignIn,
        onResend = controller::onResend,
        modifier = modifier
    )
}
