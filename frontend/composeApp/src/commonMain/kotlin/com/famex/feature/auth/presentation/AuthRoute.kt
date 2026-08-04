package com.famex.feature.auth.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer
import com.famex.core.model.AuthSession
import com.famex.core.navigation.AuthTab

@Composable
fun AuthRoute(
    initialTab: AuthTab,
    onLoggedIn: (AuthSession) -> Unit,
    onForgotPin: () -> Unit,
    onOpenBackendConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, initialTab) {
        AuthController(container.authRepository, scope).also { it.onTabChange(initialTab) }
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is AuthEvent.LoggedIn -> onLoggedIn(event.session)
            }
        }
    }

    AuthScreen(
        uiState = uiState,
        onTabChange = controller::onTabChange,
        onLoginEmailChange = controller::onLoginEmailChange,
        onLoginPinChange = controller::onLoginPinChange,
        onLogin = controller::onLogin,
        onForgotPin = onForgotPin,
        onSignUpFullNameChange = controller::onSignUpFullNameChange,
        onSignUpNicknameChange = controller::onSignUpNicknameChange,
        onSignUpEmailChange = controller::onSignUpEmailChange,
        onSignUpPinChange = controller::onSignUpPinChange,
        onSignUpPinConfirmChange = controller::onSignUpPinConfirmChange,
        onSignUp = controller::onSignUp,
        onOpenBackendConfig = onOpenBackendConfig,
        modifier = modifier
    )
}
