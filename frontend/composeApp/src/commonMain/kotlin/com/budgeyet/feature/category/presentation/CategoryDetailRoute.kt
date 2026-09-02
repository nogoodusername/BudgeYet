package com.budgeyet.feature.category.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.budgeyet.core.di.LocalAppContainer
import com.budgeyet.core.ui.DeleteCategoryDialog

@Composable
fun CategoryDetailRoute(
    categoryId: Long,
    onTransactionClick: (Long) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container, categoryId) {
        CategoryDetailController(
            categoryId,
            container.categoryRepository,
            container.transactionRepository,
            container.profileRepository,
            scope
        )
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                CategoryDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    CategoryDetailScreen(
        uiState = uiState,
        onTransactionClick = onTransactionClick,
        onDeleteCategoryClick = controller::onRequestDelete,
        modifier = modifier
    )

    if (uiState.showDeleteDialog) {
        val category = uiState.category
        if (category != null) {
            DeleteCategoryDialog(
                categoryName = category.name,
                transactionCount = uiState.transactions.size,
                reassignOptions = uiState.otherCategories,
                selectedReassignTargetId = uiState.reassignToCategoryId,
                isDeleting = uiState.isDeleting,
                errorMessage = uiState.deleteError,
                onReassignTargetSelected = controller::onReassignTargetSelected,
                onConfirm = controller::onConfirmDelete,
                onDismiss = controller::onCancelDelete
            )
        }
    }
}
