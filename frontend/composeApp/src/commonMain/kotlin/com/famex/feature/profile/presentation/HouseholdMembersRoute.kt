package com.famex.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer

@Composable
fun HouseholdMembersRoute(
    onNavigateToInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) {
        HouseholdMembersController(container.profileRepository, scope, container.currentHouseholdHolder.userId)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    HouseholdMembersScreen(
        uiState = uiState,
        onRequestRoleChange = controller::onRequestRoleChange,
        onCancelRoleChange = controller::onCancelRoleChange,
        onConfirmRoleChange = controller::onConfirmRoleChange,
        onRequestRemove = controller::onRequestRemove,
        onCancelRemove = controller::onCancelRemove,
        onConfirmRemove = controller::onConfirmRemove,
        onRevokeInvite = controller::onRevokeInvite,
        onNavigateToInvite = onNavigateToInvite,
        modifier = modifier
    )
}
