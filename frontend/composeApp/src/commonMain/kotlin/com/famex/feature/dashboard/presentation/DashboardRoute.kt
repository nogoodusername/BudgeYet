package com.famex.feature.dashboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer

@Composable
fun DashboardRoute(
    onNavigateToCategoryDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) { DashboardController(container.dashboardRepository, scope) }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is DashboardEvent.NavigateToCategoryDetail -> onNavigateToCategoryDetail(event.categoryId)
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        onCategoryClick = controller::onCategoryClick,
        onRetry = controller::retry,
        modifier = modifier
    )
}
