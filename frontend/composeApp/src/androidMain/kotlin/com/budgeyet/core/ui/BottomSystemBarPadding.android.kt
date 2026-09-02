package com.budgeyet.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// On Android the bottom system bar is the navigation bar (gesture pill or 3-button). Exclude the
// top so a caller can't accidentally pull in the status bar, and never fold in the IME.
@Composable
actual fun Modifier.bottomSystemBarPadding(): Modifier =
    windowInsetsPadding(
        WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    )
