package com.budgeyet.feature.transaction.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.budgeyet.core.model.Category
import com.budgeyet.core.model.PaymentMode
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.model.TransactionType
import com.budgeyet.core.ui.TransactionDatePickerDialog
import com.budgeyet.core.ui.categoryIcon
import com.budgeyet.core.ui.fieldColors
import com.budgeyet.core.ui.paymentModeLabel
import com.budgeyet.core.util.currencySymbolFor
import com.budgeyet.core.util.formatAmount
import com.budgeyet.core.util.todayLocalDate
import com.budgeyet.core.util.toDisplayText
import com.budgeyet.theme.BrandTeal
import com.budgeyet.theme.LocalBudgeYetTypography
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onTransactionClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onCloseFilterSheet: () -> Unit,
    onResetFilters: () -> Unit,
    onSelectDateRangeFilter: (DateRangeFilter) -> Unit,
    onTogglePayer: (Long) -> Unit,
    onTogglePaymentMode: (PaymentMode) -> Unit,
    onOpenCustomStartPicker: () -> Unit,
    onCloseCustomStartPicker: () -> Unit,
    onCustomStartSelected: (LocalDate) -> Unit,
    onOpenCustomEndPicker: () -> Unit,
    onCloseCustomEndPicker: () -> Unit,
    onCustomEndSelected: (LocalDate) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading && uiState.transactions.isEmpty() && uiState.errorMessage == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.errorMessage != null && uiState.transactions.isEmpty() ->
            Column(
                modifier = modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(uiState.errorMessage)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }

        uiState.transactions.isEmpty() -> HistoryEmptyState(onNavigateToAddTransaction, modifier)

        else -> HistoryContent(
            uiState = uiState,
            onTransactionClick = onTransactionClick,
            onSearchChange = onSearchChange,
            onOpenFilterSheet = onOpenFilterSheet,
            onResetFilters = onResetFilters,
            modifier = modifier
        )
    }

    if (uiState.showFilterSheet) {
        TransactionFilterSheet(
            uiState = uiState,
            onDismiss = onCloseFilterSheet,
            onSelectDateRangeFilter = onSelectDateRangeFilter,
            onTogglePayer = onTogglePayer,
            onTogglePaymentMode = onTogglePaymentMode,
            onOpenCustomStartPicker = onOpenCustomStartPicker,
            onOpenCustomEndPicker = onOpenCustomEndPicker,
            onApply = onCloseFilterSheet,
            onReset = onResetFilters
        )
    }

    if (uiState.showCustomStartPicker) {
        TransactionDatePickerDialog(
            selectedDate = uiState.customStartDate ?: todayLocalDate(),
            onDismiss = onCloseCustomStartPicker,
            onConfirm = onCustomStartSelected
        )
    }

    if (uiState.showCustomEndPicker) {
        TransactionDatePickerDialog(
            selectedDate = uiState.customEndDate ?: todayLocalDate(),
            onDismiss = onCloseCustomEndPicker,
            onConfirm = onCustomEndSelected
        )
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onTransactionClick: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val budgeYetType = LocalBudgeYetTypography.current
    val currencySymbol = currencySymbolFor(uiState.currency)
    val filtered = uiState.filteredTransactions
    val categoriesById = uiState.categoriesById
    val today = todayLocalDate()
    val yesterday = today.minus(1, DateTimeUnit.DAY)
    val groups = remember(filtered) {
        filtered.groupBy { transaction ->
            when (transaction.transactionDate) {
                today -> "Today, ${transaction.transactionDate.toDisplayText()}"
                yesterday -> "Yesterday, ${transaction.transactionDate.toDisplayText()}"
                else -> transaction.transactionDate.toDisplayText()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = "Search transactions", style = budgeYetType.bodyMd) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterEntryChip(
                    label = "All",
                    selected = !uiState.hasActiveFilters,
                    onClick = onResetFilters
                )
                FilterEntryChip(
                    label = if (uiState.activeFilterCount > 0) "Filters (${uiState.activeFilterCount})" else "Filters",
                    selected = uiState.hasActiveFilters,
                    icon = Icons.Default.FilterList,
                    onClick = onOpenFilterSheet
                )
            }
        }

        if (filtered.isEmpty()) {
            item { HistoryNoMatchesCard() }
        } else {
            groups.forEach { (label, transactions) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label.uppercase(), style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${formatAmount(transactions.sumOf { it.amount }, currencySymbol)} total",
                            style = budgeYetType.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(transactions, key = { it.id }) { transaction ->
                    HistoryTransactionCard(
                        transaction = transaction,
                        category = transaction.categoryId?.let { categoriesById[it] },
                        currencySymbol = currencySymbol,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterEntryChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val budgeYetType = LocalBudgeYetTypography.current
    val containerColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(1.dp, if (selected) containerColor else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
        Text(text = label, style = budgeYetType.labelMd, color = contentColor)
    }
}

@Composable
private fun HistoryTransactionCard(transaction: Transaction, category: Category?, currencySymbol: String, onClick: () -> Unit) {
    val budgeYetType = LocalBudgeYetTypography.current
    val isExpense = transaction.type == TransactionType.EXPENSE

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                        imageVector = categoryIcon(category?.icon ?: ""),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(text = transaction.merchant, style = budgeYetType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "${transaction.categoryName ?: "Uncategorized"} • ${transaction.paidBy.nickname}",
                        style = budgeYetType.labelSm,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isExpense) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                    style = budgeYetType.labelMd,
                    color = if (isExpense) MaterialTheme.colorScheme.onSurface else BrandTeal
                )
                Text(
                    text = paymentModeLabel(transaction.paymentMode),
                    style = budgeYetType.labelSm,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HistoryNoMatchesCard() {
    val budgeYetType = LocalBudgeYetTypography.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Looking for something else?", style = budgeYetType.headlineSm, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Try searching by amount, person, or specific payment method.",
            style = budgeYetType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryEmptyState(onNavigateToAddTransaction: () -> Unit, modifier: Modifier = Modifier) {
    val budgeYetType = LocalBudgeYetTypography.current

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Transactions Yet",
            style = budgeYetType.headlineLg,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When you log expenses or income, they will appear here as a shared ledger for the whole household.",
            style = budgeYetType.bodyMd,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToAddTransaction,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Log First Transaction", style = budgeYetType.labelMd)
        }
    }
}
