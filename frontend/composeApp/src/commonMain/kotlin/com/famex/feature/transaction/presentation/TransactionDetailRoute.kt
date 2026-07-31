package com.famex.feature.transaction.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer

@Composable
fun TransactionDetailRoute(transactionId: Long, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, transactionId) {
        TransactionDetailController(transactionId, container.transactionRepository, scope)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    TransactionDetailScreen(uiState = uiState, modifier = modifier)
}
