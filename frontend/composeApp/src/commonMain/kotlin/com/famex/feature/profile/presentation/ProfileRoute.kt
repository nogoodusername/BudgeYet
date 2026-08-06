package com.famex.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer
import com.famex.core.model.DisplayMode
import com.famex.core.ui.SignOutDialog

@Composable
fun ProfileRoute(
    onNavigateToManageMembers: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onDisplayModeChanged: (DisplayMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) {
        ProfileController(container.profileRepository, scope, container.currentHouseholdHolder.userId)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    ProfileScreen(
        uiState = uiState,
        onFullNameChange = controller::onFullNameChange,
        onNicknameChange = controller::onNicknameChange,
        onSaveProfile = controller::onSaveProfile,
        onCurrencyChange = controller::onCurrencyChange,
        onLanguageChange = controller::onLanguageChange,
        onDisplayModeChange = { mode ->
            controller.onDisplayModeChange(mode)
            onDisplayModeChanged(mode)
        },
        onManageMembers = onNavigateToManageMembers,
        onSignOutClick = controller::onRequestSignOut,
        modifier = modifier
    )

    if (uiState.showSignOutDialog) {
        SignOutDialog(onConfirm = onSignOut, onDismiss = controller::onCancelSignOut)
    }
}
