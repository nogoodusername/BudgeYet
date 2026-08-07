package com.budgeyet.core.navigation

import androidx.compose.runtime.Composable

// No-op — iOS has no hardware/system back button or gesture hook at this Compose Multiplatform
// version. Back navigation there is driven entirely by each screen's own back-arrow IconButton
// (OnboardingTopBar, MainAppShell's TopAppBar), so there's nothing to intercept.
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
}
