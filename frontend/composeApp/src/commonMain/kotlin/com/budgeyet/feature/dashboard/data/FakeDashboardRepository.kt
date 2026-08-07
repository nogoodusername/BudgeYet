package com.budgeyet.feature.dashboard.data

import com.budgeyet.feature.dashboard.domain.DashboardRepository
import com.budgeyet.feature.dashboard.domain.model.DashboardData
import com.budgeyet.fixtures.DummyScenario
import com.budgeyet.fixtures.dummyBudget
import com.budgeyet.fixtures.dummyCategories
import com.budgeyet.fixtures.dummyDashboardActivityFeed
import com.budgeyet.fixtures.dummyHousehold
import kotlinx.coroutines.delay

class FakeDashboardRepository(private val scenario: DummyScenario) : DashboardRepository {
    private var hasThrownOnce = false

    override suspend fun getDashboard(): DashboardData {
        delay(600)
        // SimulatedLoadingAndError forces one failure so Error/retry UI actually gets exercised.
        if (scenario == DummyScenario.SimulatedLoadingAndError && !hasThrownOnce) {
            hasThrownOnce = true
            throw IllegalStateException("Couldn't reach the household budget service.")
        }
        return DashboardData(
            household = dummyHousehold(scenario),
            budget = dummyBudget(scenario),
            categories = dummyCategories(scenario),
            activityFeed = dummyDashboardActivityFeed(scenario)
        )
    }
}
