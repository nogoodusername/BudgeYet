package com.famex.feature.dashboard.presentation

import com.famex.feature.dashboard.domain.model.DashboardData

data class DashboardUiState(
    val isLoading: Boolean = false,
    val data: DashboardData? = null,
    val errorMessage: String? = null
)

sealed class DashboardEvent {
    data class NavigateToCategoryDetail(val categoryId: Long) : DashboardEvent()
}
