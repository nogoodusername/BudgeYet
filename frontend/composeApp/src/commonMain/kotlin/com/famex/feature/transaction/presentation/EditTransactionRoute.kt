package com.famex.feature.transaction.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.famex.core.di.LocalAppContainer
import com.famex.core.ui.TransactionDatePickerDialog

@Composable
fun EditTransactionRoute(
    transactionId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, transactionId) {
        EditTransactionController(
            transactionId = transactionId,
            categoryRepository = container.categoryRepository,
            profileRepository = container.profileRepository,
            transactionRepository = container.transactionRepository,
            scope = scope
        )
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                EditTransactionEvent.Saved, EditTransactionEvent.Deleted -> onDone()
            }
        }
    }

    EditTransactionScreen(
        uiState = uiState,
        onAmountChange = controller::onAmountChange,
        onMerchantChange = controller::onMerchantChange,
        onOpenCategoryPicker = controller::onOpenCategoryPicker,
        onOpenDatePicker = controller::onOpenDatePicker,
        onPaidByChange = controller::onPaidByChange,
        onPaymentModeChange = controller::onPaymentModeChange,
        onNotesChange = controller::onNotesChange,
        onSave = controller::onSave,
        onDelete = controller::onDelete,
        modifier = modifier
    )

    if (uiState.showCategoryPicker) {
        Dialog(
            onDismissRequest = controller::onCloseCategoryPicker,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            SelectCategoryScreen(
                categories = uiState.filteredCategories,
                selectedCategoryId = uiState.selectedCategoryId,
                searchQuery = uiState.categorySearchQuery,
                onSearchQueryChange = controller::onCategorySearchChange,
                onCategorySelected = controller::onCategorySelected,
                onClose = controller::onCloseCategoryPicker
            )
        }
    }

    if (uiState.showDatePicker) {
        TransactionDatePickerDialog(
            selectedDate = uiState.selectedDate,
            onDismiss = controller::onCloseDatePicker,
            onConfirm = controller::onDateSelected
        )
    }
}
