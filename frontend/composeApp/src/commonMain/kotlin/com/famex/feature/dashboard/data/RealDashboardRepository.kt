package com.famex.feature.dashboard.data

import com.famex.core.network.HouseholdRequestContextProvider
import com.famex.core.network.mapper.toDomain
import com.famex.feature.dashboard.data.mapper.toDomain
import com.famex.feature.dashboard.data.remote.DashboardApiService
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.dashboard.domain.model.DashboardData

// Matches the "Family Activity" preview's fixture size (dummyDashboardActivityFeed takes 5) —
// this is a preview, not the full history (that's TransactionRepository/History screen).
private const val ACTIVITY_FEED_LIMIT = 5

// Real, network-backed DashboardRepository — same shape as RealCategoryRepository/
// RealTransactionRepository, resolving household id via HouseholdRequestContextProvider. Fans
// out to three endpoints (dashboard summary, household, activity feed) since no single backend
// response carries all of DashboardData — see DashboardApiService for why.
class RealDashboardRepository(
    private val api: DashboardApiService,
    private val contextProvider: HouseholdRequestContextProvider
) : DashboardRepository {

    override suspend fun getDashboard(): DashboardData {
        val (config, token, householdId) = contextProvider.get()
        val household = api.getHousehold(config, token, householdId).toDomain()
        val dashboard = api.getDashboard(config, token, householdId)
        val activityFeed = api.getActivityFeed(config, token, householdId, limit = ACTIVITY_FEED_LIMIT)

        return DashboardData(
            household = household,
            budget = dashboard.budget?.toDomain(),
            categories = dashboard.categories.map { it.toDomain() },
            activityFeed = activityFeed.items.map { it.toDomain() }
        )
    }
}
