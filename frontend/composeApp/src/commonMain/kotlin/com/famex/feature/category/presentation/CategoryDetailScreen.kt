package com.famex.feature.category.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.Category
import com.famex.core.model.SpendStatus
import com.famex.core.model.Transaction
import com.famex.core.ui.StatusProgressBar
import com.famex.core.ui.categoryIcon
import com.famex.core.ui.colorFor
import com.famex.core.ui.paymentModeLabel
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.theme.LocalFamExTypography

@Composable
fun CategoryDetailScreen(
    uiState: CategoryDetailUiState,
    onDeleteCategoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.category == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.category == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Category not found")
            }

        else -> CategoryDetailContent(
            category = uiState.category,
            transactions = uiState.transactions,
            onDeleteCategoryClick = onDeleteCategoryClick,
            modifier = modifier
        )
    }
}

@Composable
private fun CategoryDetailContent(
    category: Category,
    transactions: List<Transaction>,
    onDeleteCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencySymbol = currencySymbolFor("USD")
    val famExType = LocalFamExTypography.current
    val groups = remember(transactions) { transactions.groupBy { it.transactionDateText } }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            CategorySummaryCard(
                category = category,
                currencySymbol = currencySymbol,
                onDeleteCategoryClick = onDeleteCategoryClick
            )
        }

        item {
            Text(text = "Transactions", style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "No transactions in this category yet.",
                        style = famExType.bodyMd,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            groups.forEach { (dateLabel, groupTransactions) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateLabel.uppercase(),
                            style = famExType.labelMd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatAmount(groupTransactions.sumOf { it.amount }, currencySymbol)} total",
                            style = famExType.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(groupTransactions, key = { it.id }) { transaction ->
                    CategoryTransactionCard(transaction = transaction, category = category, currencySymbol = currencySymbol)
                }
            }
        }

        item { SpendingInsightCard(category = category, currencySymbol = currencySymbol) }
    }
}

@Composable
private fun CategorySummaryCard(category: Category, currencySymbol: String, onDeleteCategoryClick: () -> Unit) {
    val famExType = LocalFamExTypography.current
    val statusColor = colorFor(category.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon(category.icon),
                            contentDescription = category.name,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(text = category.name, style = famExType.headlineLg, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Monthly Household Budget",
                            style = famExType.bodyMd,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                CategoryAdminMenu(onDeleteCategoryClick = onDeleteCategoryClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatAmount(category.amountSpent, currencySymbol),
                    style = famExType.displayAmount,
                    color = statusColor
                )
                Text(
                    text = "of ${formatAmount(category.monthlyLimit, currencySymbol)}",
                    style = famExType.labelMd,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            StatusProgressBar(percentUsed = category.percentUsed, status = category.status, barHeight = 10.dp)
            Spacer(modifier = Modifier.height(10.dp))

            val isOverBudget = category.remainingAmount < 0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot(color = statusColor, label = "Spent: ${formatAmount(category.amountSpent, currencySymbol)}")
                LegendDot(
                    color = if (isOverBudget) statusColor else MaterialTheme.colorScheme.outlineVariant,
                    label = if (isOverBudget) "Over by ${formatAmount(-category.remainingAmount, currencySymbol)}"
                    else "Remaining: ${formatAmount(category.remainingAmount, currencySymbol)}",
                    labelColor = if (isOverBudget) statusColor else null
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            StatusBadge(status = category.status)
        }
    }
}

// Stitch "Category Detail: Groceries (Admin Menu)" screen (addc81fca0044efc9c4044026d400dd6).
// Only Delete Category is wired up — the mockup's Edit Category/Category Settings items have
// no corresponding feature in this app yet, so they're intentionally left out rather than
// shown as dead menu entries.
@Composable
private fun CategoryAdminMenu(onDeleteCategoryClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Category options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Delete Category", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onDeleteCategoryClick()
                }
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String, labelColor: Color? = null) {
    val famExType = LocalFamExTypography.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            style = famExType.labelSm,
            color = labelColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusBadge(status: SpendStatus) {
    val famExType = LocalFamExTypography.current
    val statusColor = colorFor(status)
    val (icon, label) = when (status) {
        SpendStatus.ON_TRACK -> Icons.Default.CheckCircle to "On Track"
        SpendStatus.WARNING -> Icons.Default.WarningAmber to "Near Limit"
        SpendStatus.OVER_BUDGET -> Icons.Default.ErrorOutline to "Over Budget"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
        Text(text = label, style = famExType.labelMd, color = statusColor)
    }
}

@Composable
private fun CategoryTransactionCard(transaction: Transaction, category: Category, currencySymbol: String) {
    val famExType = LocalFamExTypography.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon(category.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(text = transaction.merchant, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = transaction.paidBy.nickname.uppercase(),
                                style = famExType.labelSm.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = transaction.transactionDateText,
                            style = famExType.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${formatAmount(transaction.amount, currencySymbol)}",
                    style = famExType.labelMd,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = paymentModeLabel(transaction.paymentMode),
                    style = famExType.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpendingInsightCard(category: Category, currencySymbol: String) {
    val famExType = LocalFamExTypography.current
    val message = when (category.status) {
        SpendStatus.ON_TRACK ->
            "You're spending steadily within your ${category.name} budget this month. Keep it up!"
        SpendStatus.WARNING ->
            "You've used ${(category.percentUsed * 100).toInt()}% of your ${category.name} budget — keep an eye on upcoming purchases."
        SpendStatus.OVER_BUDGET ->
            "You've gone over your ${category.name} budget by ${formatAmount(-category.remainingAmount, currencySymbol)}. Consider adjusting your limit or pace of spending."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "Spending Insight",
                style = famExType.headlineSm,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = famExType.bodyMd,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
