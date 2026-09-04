package com.budgeyet.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.dp

// Compose Multiplatform 1.6.x doesn't wire WindowInsets.navigationBars on iOS, but it does map
// WindowInsets.safeDrawing to the UIKit safe area. There's no standalone WindowInsets.ime at
// this version, but the software keyboard IS folded into that safe area (UIKit shrinks it while
// the keyboard is up), so safeDrawing's bottom = home indicator + keyboard overlap.
//
// This is a *persistent* bottom bar: it only wants the static home-indicator inset. Keyboard
// avoidance for scrollable content is handled once, separately, by keyboardAwarePadding() (which
// reads the same LocalKeyboardOverlapHeight). Leaving the keyboard in here too made the nav bar
// jump up by the keyboard height and double-counted the inset against keyboardAwarePadding(),
// collapsing screens like History to a blank block when their search field was focused.
//
// So: take the horizontal safe-area inset as-is, but subtract the keyboard overlap back out of
// the bottom. When the keyboard is down the overlap is 0 and this is just the home indicator;
// when it's up the home indicator is behind the keyboard anyway, so clamping to 0 is fine.
@OptIn(InternalComposeApi::class)
@Composable
actual fun Modifier.bottomSystemBarPadding(): Modifier {
    val safeBottom = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val homeIndicator = (safeBottom - LocalKeyboardOverlapHeight.current).coerceAtLeast(0.dp)
    return this
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(bottom = homeIndicator)
}
