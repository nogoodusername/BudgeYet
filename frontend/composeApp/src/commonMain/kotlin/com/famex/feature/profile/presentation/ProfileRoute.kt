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
fun ProfileRoute(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) { ProfileController(container.profileRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    ProfileScreen(
        uiState = uiState,
        onFullNameChange = controller::onFullNameChange,
        onNicknameChange = controller::onNicknameChange,
        onCurrencyChange = controller::onCurrencyChange,
        onLanguageChange = controller::onLanguageChange,
        onDisplayModeChange = controller::onDisplayModeChange,
        onPushNotificationsToggle = controller::onPushNotificationsToggle,
        onCancel = controller::onCancel,
        onSaveChanges = controller::onSaveChanges,
        modifier = modifier
    )
}
