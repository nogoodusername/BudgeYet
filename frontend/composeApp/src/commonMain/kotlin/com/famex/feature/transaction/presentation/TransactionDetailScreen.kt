package com.famex.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.famex.core.model.TransactionType
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount
import com.famex.theme.BrandCoral
import com.famex.theme.BrandTeal

@Composable
fun TransactionDetailScreen(uiState: TransactionDetailUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLoading && uiState.transaction == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.transaction == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Transaction not found")
            }

        else -> {
            val transaction = uiState.transaction
            val isExpense = transaction.type == TransactionType.EXPENSE
            val currencySymbol = currencySymbolFor("USD")
            Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = transaction.merchant, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${if (isExpense) "-" else "+"}${formatAmount(transaction.amount, currencySymbol)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) BrandCoral else BrandTeal
                )
                DetailRow(label = "Category", value = transaction.categoryName ?: "Uncategorized")
                DetailRow(label = "Paid by", value = transaction.paidBy.nickname)
                DetailRow(
                    label = "Payment mode",
                    value = transaction.paymentMode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                )
                DetailRow(label = "Date", value = transaction.transactionDateText)
                transaction.notes?.let { DetailRow(label = "Notes", value = it) }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
