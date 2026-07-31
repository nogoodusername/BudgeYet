package com.famex.feature.dashboard.data

import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.dashboard.domain.model.DashboardData
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyBudget
import com.famex.fixtures.dummyCategories
import com.famex.fixtures.dummyDashboardActivityFeed
import com.famex.fixtures.dummyHousehold
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
