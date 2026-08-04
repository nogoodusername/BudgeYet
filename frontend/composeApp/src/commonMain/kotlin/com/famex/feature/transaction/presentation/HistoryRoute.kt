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
fun HistoryRoute(
    onTransactionClick: (Long) -> Unit,
    onNavigateToAddTransaction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) {
        HistoryController(container.transactionRepository, container.profileRepository, container.categoryRepository, scope)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }

    HistoryScreen(
        uiState = uiState,
        onTransactionClick = onTransactionClick,
        onRetry = controller::load,
        onSearchChange = controller::onSearchChange,
        onOpenFilterSheet = controller::onOpenFilterSheet,
        onCloseFilterSheet = controller::onCloseFilterSheet,
        onResetFilters = controller::onResetFilters,
        onSelectDateRangeFilter = controller::onSelectDateRangeFilter,
        onTogglePayer = controller::onTogglePayer,
        onTogglePaymentMode = controller::onTogglePaymentMode,
        onOpenCustomStartPicker = controller::onOpenCustomStartPicker,
        onCloseCustomStartPicker = controller::onCloseCustomStartPicker,
        onCustomStartSelected = controller::onCustomStartSelected,
        onOpenCustomEndPicker = controller::onOpenCustomEndPicker,
        onCloseCustomEndPicker = controller::onCloseCustomEndPicker,
        onCustomEndSelected = controller::onCustomEndSelected,
        onNavigateToAddTransaction = onNavigateToAddTransaction,
        modifier = modifier
    )
}
