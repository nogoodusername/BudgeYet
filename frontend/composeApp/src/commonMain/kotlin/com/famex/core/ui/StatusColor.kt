package com.famex.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.famex.core.model.SpendStatus
import com.famex.theme.BrandAmber
import com.famex.theme.BrandCoral
import com.famex.theme.BrandTeal

@Composable
@ReadOnlyComposable
fun colorFor(status: SpendStatus): Color = when (status) {
    SpendStatus.ON_TRACK -> BrandTeal
    SpendStatus.WARNING -> BrandAmber
    SpendStatus.OVER_BUDGET -> BrandCoral
}
