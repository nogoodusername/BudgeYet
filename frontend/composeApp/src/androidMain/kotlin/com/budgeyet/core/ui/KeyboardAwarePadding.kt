package com.budgeyet.core.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier

// Android reports the IME as a real window inset, so the standard modifier works. (If the
// window is in adjustResize / decor-fits-system-windows mode the framework resizes instead and
// this reads 0 — still correct, just handled a layer down.)
actual fun Modifier.keyboardAwarePadding(): Modifier = imePadding()
