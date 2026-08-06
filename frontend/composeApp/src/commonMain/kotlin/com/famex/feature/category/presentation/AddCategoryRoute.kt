package com.famex.feature.category.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.famex.core.di.LocalAppContainer

@Composable
fun AddCategoryRoute(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val controller = remember(container) {
        AddCategoryController(container.categoryRepository, container.profileRepository, scope)
    }
    val uiState by controller.uiState.collectAsState()

    LaunchedEffect(controller) { controller.load() }
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                AddCategoryEvent.Saved -> onSaved()
            }
        }
    }

    AddCategoryScreen(
        uiState = uiState,
        onNameChange = controller::onNameChange,
        onMonthlyLimitChange = controller::onMonthlyLimitChange,
        onIconSelected = controller::onIconSelected,
        onSave = controller::onSave,
        modifier = modifier
    )
}
