package com.famex.feature.transaction.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.famex.core.model.TransactionType
import com.famex.core.ui.InitialsAvatar
import com.famex.core.ui.categoryIcon
import com.famex.core.ui.paymentModeIcon
import com.famex.core.ui.paymentModeLabel
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.theme.BrandTeal
import com.famex.theme.LocalFamExTypography

/**
 * Stitch "Transaction Detail" screen (395581ac17764e89932aa7b7cc98d3cc) — a read-only view
 * separate from the edit form, reached by tapping a row in History. Edit/Delete push into
 * the existing edit screen / delete confirmation rather than editing inline.
 */
@Composable
fun TransactionDetailScreen(
    uiState: TransactionDetailUiState,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val famExType = LocalFamExTypography.current

    when {
        uiState.isLoading && uiState.transaction == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.transaction == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Transaction not found", style = famExType.bodyLg)
            }

        else -> {
            val transaction = uiState.transaction
            val currencySymbol = currencySymbolFor(uiState.currency)
            val isExpense = transaction.type == TransactionType.EXPENSE

            Column(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${if (isExpense) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                        style = famExType.displayAmount,
                        color = if (isExpense) MaterialTheme.colorScheme.onSurface else BrandTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = transaction.merchant, style = famExType.headlineSm, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Completed • ${transaction.transactionDateText}",
                            style = famExType.labelSm,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isExpense) {
                            DetailRow(
                                icon = categoryIcon(uiState.category?.icon ?: ""),
                                label = "Category",
                                value = transaction.categoryName ?: "Uncategorized"
                            )
                            DetailDivider()
                        }
                        DetailRow(icon = Icons.Default.Person, label = "Who Paid") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InitialsAvatar(name = transaction.paidBy.nickname, modifier = Modifier.size(24.dp))
                                Text(text = transaction.paidBy.nickname, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DetailDivider()
                        DetailRow(
                            icon = paymentModeIcon(transaction.paymentMode),
                            label = "Payment Mode",
                            value = paymentModeLabel(transaction.paymentMode)
                        )
                        DetailDivider()
                        DetailRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Logged by",
                            value = "${transaction.paidBy.nickname} • ${transaction.createdAtText}",
                            valueAlign = TextAlign.End
                        )

                        val notes = transaction.notes
                        if (!notes.isNullOrBlank()) {
                            DetailDivider()
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                    Text(text = "Notes", style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "\"$notes\"",
                                    style = famExType.bodyMd,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(text = "Edit Transaction", style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Delete Transaction", style = famExType.labelMd, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueAlign: TextAlign = TextAlign.Start
) {
    val famExType = LocalFamExTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Text(text = label, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = famExType.labelMd, color = MaterialTheme.colorScheme.onSurface, textAlign = valueAlign)
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    val famExType = LocalFamExTypography.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Text(text = label, style = famExType.bodyMd, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun DetailDivider() {
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
}
