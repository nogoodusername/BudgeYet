package com.famex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.data.model.Category
import com.famex.data.model.HouseholdBudgetSummary
import com.famex.data.model.Transaction
import com.famex.theme.BrandAmber
import com.famex.theme.BrandCoral
import com.famex.theme.BrandTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: HouseholdBudgetSummary,
    categories: List<Category>,
    activityFeed: List<Transaction>,
    onAddTransactionClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "fam-ex Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = BrandTeal,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Overview Budget Gauge Card (PRD B1)
            item {
                BudgetOverviewGaugeCard(summary = summary)
            }

            // Section 2: Category Snapshots Header (PRD B3)
            item {
                Text(
                    text = "Category Snapshots",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(categories) { category ->
                CategorySnapshotRow(category = category, currencySymbol = summary.currencySymbol)
            }

            // Section 3: Family Activity Feed (PRD B4)
            item {
                Text(
                    text = "Family Activity Feed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            items(activityFeed) { transaction ->
                ActivityFeedItem(transaction = transaction, currencySymbol = summary.currencySymbol)
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun BudgetOverviewGaugeCard(summary: HouseholdBudgetSummary) {
    val progressColor = when {
        summary.percentageUtilized >= 1.0f -> BrandCoral
        summary.percentageUtilized >= 0.75f -> BrandAmber
        else -> BrandTeal
    }

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
                text = summary.budgetName,
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
                    text = "${summary.currencySymbol}${summary.totalSpentAmount.toInt()} spent",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "of ${summary.currencySymbol}${summary.totalGoalAmount.toInt()}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Linear Progress Bar (PRD Section B1)
            LinearProgressIndicator(
                progress = { summary.percentageUtilized.coerceAtMost(1.0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Text(
                text = if (summary.remainingAmount >= 0)
                    "${summary.currencySymbol}${summary.remainingAmount.toInt()} remaining"
                else
                    "Over budget by ${summary.currencySymbol}${(-summary.remainingAmount).toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.remainingAmount >= 0) BrandTeal else BrandCoral
            )
        }
    }
}

@Composable
fun CategorySnapshotRow(category: Category, currencySymbol: String) {
    val barColor = when {
        category.percentageUtilized >= 1.0f -> BrandCoral
        category.percentageUtilized >= 0.75f -> BrandAmber
        else -> BrandTeal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = category.name,
                        tint = BrandTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = category.name,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${currencySymbol}${category.amountSpent.toInt()} / ${currencySymbol}${category.monthlyLimit.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LinearProgressIndicator(
                progress = { category.percentageUtilized.coerceAtMost(1.0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun ActivityFeedItem(transaction: Transaction, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transaction.paidByNickname.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal
                    )
                }

                Column {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${transaction.paidByNickname} • ${transaction.categoryName} • ${transaction.dateText}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "${if (transaction.isExpense) "-" else "+"}${currencySymbol}${transaction.amount.toInt()}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (transaction.isExpense) BrandCoral else BrandTeal
            )
        }
    }
}
