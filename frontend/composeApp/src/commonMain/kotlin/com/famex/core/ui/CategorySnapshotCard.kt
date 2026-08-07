package com.famex.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.Category
import com.famex.core.model.SpendStatus
import com.famex.theme.LocalFamExTypography
import kotlin.math.roundToInt

@Composable
fun CategorySnapshotCard(
    category: Category,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val statusColor = colorFor(category.status)
    val onTrack = category.status == SpendStatus.ON_TRACK
    val cardModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    val famExType = LocalFamExTypography.current

    Card(
        modifier = cardModifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category.icon),
                        contentDescription = category.name,
                        tint = statusColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${(category.percentUsed * 100).roundToInt()}%",
                    style = famExType.labelSm.copy(fontWeight = if (onTrack) FontWeight.Medium else FontWeight.SemiBold),
                    color = if (onTrack) MaterialTheme.colorScheme.onSurfaceVariant else statusColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = category.name,
                        style = famExType.labelMd,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category.status == SpendStatus.OVER_BUDGET) {
                        Text(
                            text = "Over Budget",
                            style = famExType.labelSm.copy(fontSize = 10.sp),
                            color = statusColor
                        )
                    }
                }
                StatusProgressBar(percentUsed = category.percentUsed, status = category.status, barHeight = 6.dp)
            }
        }
    }
}

@Composable
fun AddCategoryPlaceholderCard(modifier: Modifier = Modifier) {
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .dashedBorder(color = outlineVariant, strokeWidth = 1.dp, cornerRadius = 12.dp)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Category",
            tint = onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add Category",
            style = LocalFamExTypography.current.labelMd,
            color = onSurfaceVariant
        )
    }
}
