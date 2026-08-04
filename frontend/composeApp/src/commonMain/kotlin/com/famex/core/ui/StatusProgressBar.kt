package com.famex.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.famex.core.model.SpendStatus

@Composable
fun StatusProgressBar(
    percentUsed: Float,
    status: SpendStatus,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp
) {
    LinearProgressIndicator(
        progress = { percentUsed.coerceAtMost(1.0f) },
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(barHeight / 2)),
        color = colorFor(status),
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    )
}
