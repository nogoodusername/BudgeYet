package com.budgeyet.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.budgeyet.core.model.SpendStatus
import com.budgeyet.theme.BrandAmber
import com.budgeyet.theme.BrandTeal

@Composable
@ReadOnlyComposable
fun colorFor(status: SpendStatus): Color = when (status) {
    SpendStatus.ON_TRACK -> BrandTeal
    SpendStatus.WARNING -> BrandAmber
    SpendStatus.OVER_BUDGET -> MaterialTheme.colorScheme.error
}
