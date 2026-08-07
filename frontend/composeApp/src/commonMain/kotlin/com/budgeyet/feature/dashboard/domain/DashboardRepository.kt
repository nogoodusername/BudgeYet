package com.budgeyet.feature.dashboard.domain

import com.budgeyet.feature.dashboard.domain.model.DashboardData

interface DashboardRepository {
    suspend fun getDashboard(): DashboardData
}
