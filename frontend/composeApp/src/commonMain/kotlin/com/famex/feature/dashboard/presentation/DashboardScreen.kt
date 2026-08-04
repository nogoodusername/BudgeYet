package com.famex.feature.dashboard.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.model.Category
import com.famex.core.model.SpendStatus
import com.famex.core.ui.ActivityFeedRow
import com.famex.core.ui.AddCategoryPlaceholderCard
import com.famex.core.ui.CategorySnapshotCard
import com.famex.core.ui.StatusProgressBar
import com.famex.core.ui.colorFor
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.feature.dashboard.domain.model.DashboardData
import com.famex.theme.LocalFamExTypography

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onCategoryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onViewAllActivityClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.data == null -> DashboardLoading(modifier)
        uiState.errorMessage != null && uiState.data == null -> DashboardError(uiState.errorMessage, onRetry, modifier)
        uiState.data != null -> DashboardContent(uiState.data, onCategoryClick, onViewAllActivityClick, modifier)
    }
}

@Composable
private fun DashboardLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    onCategoryClick: (Long) -> Unit,
    onViewAllActivityClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol = currencySymbolFor(data.household.currency)
    val famExType = LocalFamExTypography.current
    val categoriesById = remember(data.categories) { data.categories.associateBy { it.id } }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            val budget = data.budget
            if (budget != null) {
                BudgetOverviewCard(
                    title = monthYearLabel(budget.month, budget.year),
                    spent = budget.spentAmount,
                    goal = budget.monthlyGoalAmount,
                    remaining = budget.remainingAmount,
                    percentUsed = budget.percentUsed,
                    status = budget.status,
                    currencySymbol = currencySymbol
                )
            } else {
                NoBudgetCard()
            }
        }

        if (data.categories.isNotEmpty()) {
            item {
                Text(
                    text = "Category Snapshots",
                    style = famExType.headlineSm,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val slots: List<Category?> = data.categories + listOf(null)
            items(slots.chunked(2)) { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSlots.forEach { slot ->
                        Box(modifier = Modifier.weight(1f)) {
                            if (slot != null) {
                                CategorySnapshotCard(category = slot, onClick = { onCategoryClick(slot.id) })
                            } else {
                                AddCategoryPlaceholderCard()
                            }
                        }
                    }
                    if (rowSlots.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Family Activity",
                    style = famExType.headlineSm,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onViewAllActivityClick) {
                    Text(text = "View All", style = famExType.labelMd, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        if (data.activityFeed.isEmpty()) {
            item { EmptyActivityCard() }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        data.activityFeed.forEachIndexed { index, transaction ->
                            ActivityFeedRow(
                                transaction = transaction,
                                category = transaction.categoryId?.let { categoriesById[it] },
                                currencySymbol = currencySymbol
                            )
                            if (index != data.activityFeed.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetOverviewCard(
    title: String,
    spent: Double,
    goal: Double,
    remaining: Double,
    percentUsed: Float,
    status: SpendStatus,
    currencySymbol: String
) {
    val famExType = LocalFamExTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatAmount(spent, currencySymbol),
                        style = famExType.displayAmount,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of ${formatAmount(goal, currencySymbol)}",
                        style = famExType.labelMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                StatusProgressBar(percentUsed = percentUsed, status = status, barHeight = 8.dp)

                Text(
                    text = if (remaining >= 0) "${formatAmount(remaining, currencySymbol)} remaining"
                    else "Over budget by ${formatAmount(-remaining, currencySymbol)}",
                    style = famExType.labelMd,
                    color = colorFor(status),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun NoBudgetCard() {
    val famExType = LocalFamExTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "No budget set up yet", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Set a monthly goal to start tracking your household spending.",
                style = famExType.bodyMd,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyActivityCard() {
    val famExType = LocalFamExTypography.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "No transactions yet. Tap + to add your first one.",
            style = famExType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private fun monthYearLabel(month: Int, year: Int): String {
    val name = monthNames.getOrNull(month - 1) ?: "Month"
    return "$name $year Budget"
}
