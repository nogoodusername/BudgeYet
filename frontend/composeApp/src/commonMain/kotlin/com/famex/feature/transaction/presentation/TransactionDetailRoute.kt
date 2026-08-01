package com.famex.feature.transaction.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer
import com.famex.core.ui.DeleteTransactionDialog
import com.famex.core.util.currencySymbolFor

@Composable
fun TransactionDetailRoute(
    transactionId: Long,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, transactionId) {
        TransactionDetailController(
            transactionId = transactionId,
            transactionRepository = container.transactionRepository,
            categoryRepository = container.categoryRepository,
            scope = scope
        )
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                TransactionDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    TransactionDetailScreen(
        uiState = uiState,
        onEdit = { onEdit(transactionId) },
        onDeleteClick = controller::onRequestDelete,
        modifier = modifier
    )

    if (uiState.showDeleteConfirm) {
        val transaction = uiState.transaction
        if (transaction != null) {
            DeleteTransactionDialog(
                merchant = transaction.merchant,
                amount = transaction.amount,
                currencySymbol = currencySymbolFor("USD"),
                isDeleting = uiState.isDeleting,
                onConfirm = controller::onConfirmDelete,
                onDismiss = controller::onCancelDelete
            )
        }
    }
}
