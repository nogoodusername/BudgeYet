package com.budgeyet.feature.dashboard

import com.budgeyet.core.network.AuthTokenStorage
import com.budgeyet.core.network.BackendConfigStorage
import com.budgeyet.core.network.HouseholdRequestContextProvider
import com.budgeyet.core.persistence.SettingsStorage
import com.budgeyet.core.session.CurrentHouseholdHolder
import com.budgeyet.feature.dashboard.data.RealDashboardRepository
import com.budgeyet.feature.dashboard.data.remote.DashboardApiService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class InMemorySettings : SettingsStorage {
    private val map = mutableMapOf<String, String>()
    override suspend fun getString(key: String) = map[key]
    override suspend fun putString(key: String, value: String) { map[key] = value }
    override suspend fun remove(key: String) { map.remove(key) }
}

private const val HOUSEHOLD_JSON =
    """{"id":1,"name":"Casa","currency":"USD","language":"en","cycle_start_day":1,"members":[],"created_at":"2026-01-01T00:00:00"}"""
private const val ACTIVITY_JSON = """{"items":[],"total":0,"limit":5,"offset":0}"""
private const val DASHBOARD_NO_BUDGET =
    """{"has_budget":false,"has_transactions":false,"budget":null,"categories":[]}"""
private const val DASHBOARD_WITH_BUDGET =
    """{"has_budget":true,"has_transactions":false,"budget":{"id":42,"name":"September 2026 Budget","monthly_goal_amount":"1234.00","spent":"0.00","month":9,"year":2026},"categories":[]}"""

/**
 * Drives [RealDashboardRepository] against a [MockEngine].
 *
 * @param budgetPresentInitially when true, the very first `GET /dashboard` already has a budget.
 * @param rolloverBody body returned by `POST /budgets/rollover` — `null` means the endpoint
 *   responded with a JSON `null` (household never had a budget). Once rollover has been called
 *   with a non-null body, subsequent `GET /dashboard` calls return a budget.
 */
private class Harness(
    budgetPresentInitially: Boolean = false,
    private val rolloverBody: String? = null,
) {
    var dashboardCalls = 0
        private set
    var rolloverCalls = 0
        private set

    private val settings = InMemorySettings()

    private val engine = MockEngine { request ->
        val path = request.url.encodedPath
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        when {
            path.endsWith("/budgets/rollover") && request.method == HttpMethod.Post -> {
                rolloverCalls++
                respond(rolloverBody ?: "null", HttpStatusCode.OK, jsonHeader)
            }
            path.endsWith("/dashboard") -> {
                dashboardCalls++
                val hasBudget = budgetPresentInitially || (rolloverCalls > 0 && rolloverBody != null)
                respond(
                    if (hasBudget) DASHBOARD_WITH_BUDGET else DASHBOARD_NO_BUDGET,
                    HttpStatusCode.OK,
                    jsonHeader,
                )
            }
            path.endsWith("/activity-feed") -> respond(ACTIVITY_JSON, HttpStatusCode.OK, jsonHeader)
            path.endsWith("/households/1") -> respond(HOUSEHOLD_JSON, HttpStatusCode.OK, jsonHeader)
            else -> respond("""{"detail":"unexpected ${'$'}path"}""", HttpStatusCode.NotFound, jsonHeader)
        }
    }

    private val client = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    }

    suspend fun repository(): RealDashboardRepository {
        AuthTokenStorage(settings).setToken("test-token")
        val provider = HouseholdRequestContextProvider(
            tokenStorage = AuthTokenStorage(settings),
            backendConfigStorage = BackendConfigStorage(settings),
            householdHolder = CurrentHouseholdHolder().apply { householdId = 1 },
        )
        return RealDashboardRepository(DashboardApiService(client), provider)
    }
}

class RealDashboardRepositoryTest {

    @Test
    fun carriesBudgetForwardWhenCurrentCycleHasNone() = runTest {
        val h = Harness(rolloverBody = """{"id":42}""")

        val data = h.repository().getDashboard()

        assertEquals(1, h.rolloverCalls)
        assertEquals(2, h.dashboardCalls) // initial fetch + re-fetch after rollover
        assertNotNull(data.budget)
        assertEquals(1234.0, data.budget!!.monthlyGoalAmount)
    }

    @Test
    fun doesNotCallRolloverWhenBudgetAlreadyPresent() = runTest {
        val h = Harness(budgetPresentInitially = true)

        val data = h.repository().getDashboard()

        assertEquals(0, h.rolloverCalls)
        assertEquals(1, h.dashboardCalls)
        assertNotNull(data.budget)
    }

    @Test
    fun leavesEmptyStateWhenNothingToCarry() = runTest {
        val h = Harness(rolloverBody = null) // endpoint responds with JSON null

        val data = h.repository().getDashboard()

        assertEquals(1, h.rolloverCalls)
        assertEquals(1, h.dashboardCalls) // no re-fetch
        assertNull(data.budget)
    }
}
