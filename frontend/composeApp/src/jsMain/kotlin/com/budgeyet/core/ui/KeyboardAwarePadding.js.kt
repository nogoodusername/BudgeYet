package com.budgeyet.core.ui

import androidx.compose.ui.Modifier

// Browser: the page/soft keyboard is the OS's concern, and there's no Compose IME inset to
// react to. No-op.
actual fun Modifier.keyboardAwarePadding(): Modifier = this
