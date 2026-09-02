package com.budgeyet.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight

// Compose Multiplatform 1.6.x has no WindowInsets.ime on iOS, so imePadding() does nothing.
// LocalKeyboardOverlapHeight (@InternalComposeApi, but the only option at this version) carries
// the animated height by which the keyboard overlaps the Compose view — pad the container's
// bottom with it so scrollable content can move clear of the keyboard.
@OptIn(InternalComposeApi::class)
actual fun Modifier.keyboardAwarePadding(): Modifier = composed {
    padding(bottom = LocalKeyboardOverlapHeight.current)
}
