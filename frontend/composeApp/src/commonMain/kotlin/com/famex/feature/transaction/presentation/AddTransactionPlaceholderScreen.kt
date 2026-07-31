package com.famex.feature.transaction.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// The Add Transaction form (D1: amount, merchant, category, payer, payment mode, future-date
// rejection) is out of scope for this pass — this screen only proves the FAB → nav wiring.
@Composable
fun AddTransactionPlaceholderScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "The add-transaction form lands in a follow-up build.",
            textAlign = TextAlign.Center
        )
    }
}
