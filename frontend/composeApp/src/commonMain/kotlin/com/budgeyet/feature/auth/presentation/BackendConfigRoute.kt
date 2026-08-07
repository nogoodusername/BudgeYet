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
fun BackendConfigRoute(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) { BackendConfigController(container.authRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                BackendConfigEvent.Saved -> onSaved()
            }
        }
    }

    BackendConfigScreen(
        uiState = uiState,
        onSelectHosted = controller::onSelectHosted,
        onSelectCustom = controller::onSelectCustom,
        onCustomUrlChange = controller::onCustomUrlChange,
        onSave = controller::onSave,
        modifier = modifier
    )
}
