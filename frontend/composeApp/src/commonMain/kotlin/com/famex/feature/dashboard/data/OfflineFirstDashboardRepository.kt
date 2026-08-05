package com.famex.feature.dashboard.data

import com.famex.core.cache.LocalCacheStore
import com.famex.core.offline.networkFirstRead
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.dashboard.domain.model.DashboardData

// Read-only, so this wrapper is nothing but read-through caching: getDashboard tries the real
// repo first and mirrors the result into the cache; on connectivity failure it serves the cached
// snapshot (a mix of Household + Budget + categories + activity feed, all serialized as one blob).
// A stale-but-recent dashboard is a much better offline experience than a full-screen error.
class OfflineFirstDashboardRepository(
    private val delegate: DashboardRepository,
    private val cacheStore: LocalCacheStore
) : DashboardRepository {

    override suspend fun getDashboard(): DashboardData = networkFirstRead(
        networkCall = { delegate.getDashboard() },
        cached = { cacheStore.getCachedDashboardData() },
        onSuccess = { cacheStore.cacheDashboardData(it) }
    )
}
