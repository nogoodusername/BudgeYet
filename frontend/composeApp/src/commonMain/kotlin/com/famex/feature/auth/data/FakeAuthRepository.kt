package com.famex.feature.auth.data

import com.famex.core.model.AuthSession
import com.famex.core.model.BackendConfig
import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole
import com.famex.core.model.User
import com.famex.core.util.todayLocalDate
import com.famex.feature.auth.domain.AuthRepository
import com.famex.feature.auth.domain.CategorySetupInput
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCurrentUser
import com.famex.fixtures.dummyHousehold
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlin.random.Random

// Deliberately decoupled from the other Fake*Repository instances in AppContainer
// (Dashboard/Category/Transaction/Profile) — those always serve AppContainer's fixed
// DummyScenario regardless of which account signs in here, matching the rest of the app's
// "dummy data until real networking" stance. Sign in as the seeded demo account (below) to see
// that scenario data end to end; a fresh sign-up gets its own isolated in-memory household that
// only the onboarding screens see.
class FakeAuthRepository(scenario: DummyScenario, private val httpClient: HttpClient) : AuthRepository {
    private class Account(var user: User, var pin: String, var household: Household?)

    private val demoUser = dummyCurrentUser()
    private val accounts = mutableMapOf(
        demoUser.email to Account(user = demoUser, pin = DEMO_PIN, household = dummyHousehold(scenario))
    )
    private var nextUserId = 1000L
    private var nextHouseholdId = 1000L
    private var backendConfig: BackendConfig = BackendConfig.Hosted

    // A single shared joinable household that "Join a Household" adds members to, capped at
    // Household.MAX_MEMBERS same as the real 3-member household cap.
    private var joinableHousehold = Household(
        id = 500,
        name = "Henderson Family Budget",
        currency = "USD",
        language = "en",
        cycleStartDay = 1,
        members = listOf(
            HouseholdMember(
                id = 1,
                user = User(id = 501, email = "owner@henderson.example", fullName = "Jordan Henderson", nickname = "Jordan"),
                role = MemberRole.OWNER,
                joinedAtText = "Jan 2026"
            )
        ),
        joinCodeExpiresAt = todayLocalDate().plus(Household.JOIN_CODE_EXPIRY_DAYS, DateTimeUnit.DAY)
    )

    override suspend fun signUp(fullName: String, nickname: String, email: String, pin: String) {
        delay(400)
        if (accounts.containsKey(email)) throw IllegalStateException("An account with this email already exists")
        if (!pin.matches(Regex("^\\d{6}$"))) throw IllegalArgumentException("PIN must be 6 digits")
        val user = User(
            id = nextUserId++,
            email = email,
            fullName = fullName,
            nickname = nickname.ifBlank { fullName.substringBefore(" ") }
        )
        // No email sent (and nothing to log) — the PIN is user-chosen, they already know it.
        // Mirrors AuthService.signup on the backend, which stopped generating one too.
        accounts[email] = Account(user = user, pin = pin, household = null)
    }

    override suspend fun login(email: String, pin: String): AuthSession {
        delay(400)
        val account = accounts[email]
        if (account == null || account.pin != pin) throw IllegalArgumentException("Invalid email or PIN")
        return AuthSession(user = account.user, household = account.household)
    }

    override suspend fun requestPinReset(email: String) {
        delay(400)
        val account = accounts[email] ?: return
        account.pin = generatePin()
        println("[FakeAuthRepository] New PIN for $email: ${account.pin}")
    }

    override suspend fun createHousehold(email: String, name: String, currency: String, cycleStartDay: Int): Household {
        delay(400)
        val account = accounts[email] ?: throw IllegalStateException("No account found for $email")
        val household = Household(
            id = nextHouseholdId++,
            name = name,
            currency = currency,
            language = "en",
            cycleStartDay = cycleStartDay,
            members = listOf(HouseholdMember(id = 1, user = account.user, role = MemberRole.OWNER, joinedAtText = "Just now")),
            joinCodeExpiresAt = todayLocalDate().plus(Household.JOIN_CODE_EXPIRY_DAYS, DateTimeUnit.DAY)
        )
        account.household = household
        return household
    }

    override suspend fun joinHousehold(email: String, inviteCode: String): Household {
        delay(400)
        val account = accounts[email] ?: throw IllegalStateException("No account found for $email")
        if (inviteCode.isBlank()) throw IllegalArgumentException("Enter an invite code")
        if (joinableHousehold.members.size >= Household.MAX_MEMBERS) {
            throw IllegalStateException("This household is already full")
        }
        joinableHousehold = joinableHousehold.copy(
            members = joinableHousehold.members + HouseholdMember(
                id = (joinableHousehold.members.maxOfOrNull { it.id } ?: 0) + 1,
                user = account.user,
                role = MemberRole.MEMBER,
                joinedAtText = "Just now"
            )
        )
        account.household = joinableHousehold
        return joinableHousehold
    }

    // No-ops beyond the delay + household lookup — same "decoupled from the fixed
    // DummyScenario the rest of the app serves" stance as createHousehold/joinHousehold above.
    // Real persistence lands with the networking layer; for now this just validates the
    // household exists and lets the onboarding funnel proceed.
    override suspend fun setupBudget(householdId: Long, name: String, period: String, monthlyGoalAmount: Double) {
        delay(400)
        accounts.values.find { it.household?.id == householdId }
            ?: throw IllegalStateException("No household found for id $householdId")
    }

    override suspend fun setupCategories(householdId: Long, categories: List<CategorySetupInput>) {
        delay(400)
        accounts.values.find { it.household?.id == householdId }
            ?: throw IllegalStateException("No household found for id $householdId")
    }

    override suspend fun getBackendConfig(): BackendConfig {
        delay(100)
        return backendConfig
    }

    override suspend fun setBackendConfig(config: BackendConfig) {
        delay(200)
        backendConfig = config
    }

    // The DB-independent /ping alias (see backend/app/api/v1/endpoints/health.py) — reachable
    // even if the target server's database isn't configured, which is exactly what "is this URL
    // a live Fam-Ex server" needs to check, independent of whether it's fully set up yet.
    override suspend fun checkServerReachable(url: String) {
        val pingUrl = "${url.trim().trimEnd('/')}/api/v1/ping"
        try {
            val response = httpClient.get(pingUrl)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Server responded with ${response.status.value}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Server unreachable")
        }
    }
}

private fun generatePin(): String = (1..6).joinToString("") { Random.nextInt(0, 10).toString() }

// Fixed PIN for the seeded demo account (alex@example.com) — a fake-repo-only convenience so
// the app can be previewed fully signed-in without reading console output first.
private const val DEMO_PIN = "123456"
