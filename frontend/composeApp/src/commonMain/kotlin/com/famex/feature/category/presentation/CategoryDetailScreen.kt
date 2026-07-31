package com.famex.feature.category.presentation

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
import com.famex.core.ui.StatusProgressBar
import com.famex.core.ui.colorFor
import com.famex.core.util.currencySymbolFor
import com.famex.core.util.formatAmount

@Composable
fun CategoryDetailScreen(uiState: CategoryDetailUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLoading && uiState.category == null ->
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        uiState.category == null ->
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Category not found")
            }

        else -> {
            val category = uiState.category
            val currencySymbol = currencySymbolFor("USD")
            Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = category.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${formatAmount(category.amountSpent, currencySymbol)} spent of ${formatAmount(category.monthlyLimit, currencySymbol)}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                StatusProgressBar(percentUsed = category.percentUsed, status = category.status, barHeight = 10.dp)
                Text(
                    text = if (category.remainingAmount >= 0) "${formatAmount(category.remainingAmount, currencySymbol)} remaining"
                    else "Over limit by ${formatAmount(-category.remainingAmount, currencySymbol)}",
                    fontWeight = FontWeight.SemiBold,
                    color = colorFor(category.status)
                )
            }
        }
    }
}
