package com.budgeyet.feature.auth.data

import com.budgeyet.core.model.AuthSession
import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.model.Household
import com.budgeyet.core.network.AppException
import com.budgeyet.core.network.AuthTokenStorage
import com.budgeyet.core.network.BackendConfigStorage
import com.budgeyet.core.network.apiUrl
import com.budgeyet.core.network.mapper.toDomain
import com.budgeyet.core.persistence.SettingsStorage
import com.budgeyet.feature.auth.data.remote.AuthApiService
import com.budgeyet.feature.auth.domain.AuthRepository
import com.budgeyet.feature.auth.domain.CategorySetupInput
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Real, network-backed AuthRepository — talks to the FastAPI backend via AuthApiService instead
// of FakeAuthRepository's in-memory account map. Session persistence and the reachability ping
// are unchanged from FakeAuthRepository, since those were never fake to begin with — see its
// comments for why they're shaped this way. BackendConfig storage moved out to
// core/network/BackendConfigStorage.kt since other Real*Repository implementations need it too.
class RealAuthRepository(
    private val httpClient: HttpClient,
    private val api: AuthApiService,
    private val settingsStorage: SettingsStorage,
    private val tokenStorage: AuthTokenStorage,
    private val backendConfigStorage: BackendConfigStorage
) : AuthRepository {

    override suspend fun signUp(fullName: String, nickname: String, email: String, pin: String) {
        api.signUp(getBackendConfig(), fullName, nickname, email, pin)
    }

    override suspend fun login(email: String, pin: String): AuthSession {
        val config = getBackendConfig()
        val response = api.login(config, email, pin)
        tokenStorage.setToken(response.accessToken)
        val household = api.getMyHousehold(config, response.accessToken)?.toDomain()
        return AuthSession(user = response.user.toDomain(), household = household)
    }

    override suspend fun requestPinReset(email: String) {
        api.forgotPin(getBackendConfig(), email)
    }

    override suspend fun createHousehold(email: String, name: String, currency: String, cycleStartDay: Int): Household {
        val config = getBackendConfig()
        val token = currentAccessToken()
        return api.createHousehold(config, token, name, currency, cycleStartDay).toDomain()
    }

    override suspend fun joinHousehold(email: String, inviteCode: String): Household {
        val config = getBackendConfig()
        val token = currentAccessToken()
        val membership = api.joinHousehold(config, token, inviteCode)
        // HouseholdMemberResponse only describes the caller's own membership — fetch the full
        // household (all members) the same way a fresh login resolves one.
        return api.getHousehold(config, token, membership.householdId).toDomain()
    }

    override suspend fun setupBudget(householdId: Long, name: String, monthlyGoalAmount: Double) {
        val config = getBackendConfig()
        api.createBudget(config, currentAccessToken(), householdId, name, monthlyGoalAmount)
    }

    override suspend fun setupCategories(householdId: Long, categories: List<CategorySetupInput>) {
        val config = getBackendConfig()
        val token = currentAccessToken()
        // One POST per category — mirrors what feature/category's Add Category flow does
        // against the same endpoint; there's no batch-create endpoint on the backend.
        for (category in categories) {
            api.createCategory(config, token, householdId, category.name, category.icon, category.monthlyLimit)
        }
    }

    override suspend fun getBackendConfig(): BackendConfig = backendConfigStorage.get()

    override suspend fun setBackendConfig(config: BackendConfig) = backendConfigStorage.set(config)

    // Same DB-independent /ping liveness check as before real networking existed elsewhere —
    // see backend/app/api/v1/endpoints/health.py.
    override suspend fun checkServerReachable(url: String) {
        val pingUrl = BackendConfig.Custom(url).apiUrl("/ping")
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

    override suspend fun getPersistedSession(): AuthSession? {
        val json = settingsStorage.getString(KEY_SESSION) ?: return null
        return try {
            appJson.decodeFromString<AuthSession>(json)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun persistSession(session: AuthSession) {
        settingsStorage.putString(KEY_SESSION, appJson.encodeToString(session))
    }

    override suspend fun clearPersistedSession() {
        settingsStorage.remove(KEY_SESSION)
        tokenStorage.clearToken()
    }

    // Every authenticated call goes through here rather than each call site re-fetching+null
    // checking — a null token means the UI let an authenticated screen render without a
    // completed login, which is a bug upstream, not a recoverable network condition.
    private suspend fun currentAccessToken(): String =
        tokenStorage.getToken() ?: throw AppException.AuthenticationException(
            "Your session has expired. Please sign in again."
        )
}

private const val KEY_SESSION = "auth_session"

private val appJson = Json { ignoreUnknownKeys = true }
