package com.famex.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.SpendStatus
import com.famex.core.ui.CategoryRow
import com.famex.core.ui.StatusProgressBar
import com.famex.core.ui.TransactionRow
import com.famex.core.ui.colorFor
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.feature.dashboard.domain.model.DashboardData

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onCategoryClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.data == null -> DashboardLoading(modifier)
        uiState.errorMessage != null && uiState.data == null -> DashboardError(uiState.errorMessage, onRetry, modifier)
        uiState.data != null -> DashboardContent(uiState.data, onCategoryClick, modifier)
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
    modifier: Modifier = Modifier
) {
    val currencySymbol = currencySymbolFor(data.household.currency)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val budget = data.budget
            if (budget != null) {
                BudgetOverviewGaugeCard(
                    budgetName = budget.name,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(data.categories) { category ->
                CategoryRow(category = category, currencySymbol = currencySymbol, onClick = { onCategoryClick(category.id) })
            }
        }

        item {
            Text(
                text = "Family Activity Feed",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (data.activityFeed.isEmpty()) {
            item {
                Text(
                    text = "No transactions yet. Tap + to add your first one.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        } else {
            items(data.activityFeed) { transaction ->
                TransactionRow(transaction = transaction, currencySymbol = currencySymbol)
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BudgetOverviewGaugeCard(
    budgetName: String,
    spent: Double,
    goal: Double,
    remaining: Double,
    percentUsed: Float,
    status: SpendStatus,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = budgetName,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${formatAmount(spent, currencySymbol)} spent",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of ${formatAmount(goal, currencySymbol)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            StatusProgressBar(percentUsed = percentUsed, status = status, barHeight = 10.dp)

            Text(
                text = if (remaining >= 0) "${formatAmount(remaining, currencySymbol)} remaining"
                else "Over budget by ${formatAmount(-remaining, currencySymbol)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorFor(status)
            )
        }
    }
}

@Composable
private fun NoBudgetCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "No budget set up yet",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Set a monthly goal to start tracking your household spending.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
