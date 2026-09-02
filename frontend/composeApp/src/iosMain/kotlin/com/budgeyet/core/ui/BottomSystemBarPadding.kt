package com.budgeyet.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Compose Multiplatform 1.6.x doesn't wire WindowInsets.navigationBars on iOS, but it does map
// WindowInsets.safeDrawing to the UIKit safe area — the bottom of which is the home indicator.
// There's no WindowInsets.ime on iOS at this version (keyboard avoidance is handled separately
// via keyboardAwarePadding), so safeDrawing's bottom is purely the home indicator here.
@Composable
actual fun Modifier.bottomSystemBarPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    )
