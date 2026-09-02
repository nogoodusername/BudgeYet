package com.budgeyet.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Browser: no OS navigation bar / home indicator inside the viewport, and Compose for Web has no
// window insets to react to. No-op — mirrors keyboardAwarePadding on this target.
@Composable
actual fun Modifier.bottomSystemBarPadding(): Modifier = this
