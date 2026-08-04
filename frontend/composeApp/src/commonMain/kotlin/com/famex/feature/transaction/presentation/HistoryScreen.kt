package com.famex.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.famex.core.ui.TransactionRow
import com.famex.core.util.currencySymbolFor

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onTransactionClick: (Long) -> Unit,
    onRetry: () -> Unit,
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

        uiState.transactions.isEmpty() ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No transactions yet.")
            }

        else -> {
            val currencySymbol = currencySymbolFor("USD")
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.transactions) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        onClick = { onTransactionClick(transaction.id) }
                    )
                }
            }
        }
    }
}
