package com.famex.feature.dashboard.domain

import com.famex.feature.dashboard.domain.model.DashboardData

interface DashboardRepository {
    suspend fun getDashboard(): DashboardData
}
