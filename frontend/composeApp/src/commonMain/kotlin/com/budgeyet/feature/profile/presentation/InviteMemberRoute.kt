package com.budgeyet.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.budgeyet.core.di.LocalAppContainer

@Composable
fun InviteMemberRoute(
    onInvited: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) { InviteMemberController(container.profileRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                InviteMemberEvent.Invited -> onInvited()
            }
        }
    }

    InviteMemberScreen(
        uiState = uiState,
        onEmailChange = controller::onEmailChange,
        onSendInvite = controller::onSendInvite,
        modifier = modifier
    )
}
