package com.famex.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.famex.feature.auth.data.RealAuthRepository
import com.famex.feature.auth.data.remote.AuthApiService
import com.famex.feature.auth.domain.AuthRepository
import com.famex.feature.category.data.RealCategoryRepository
import com.famex.feature.category.data.remote.CategoryApiService
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.dashboard.data.RealDashboardRepository
import com.famex.feature.dashboard.data.remote.DashboardApiService
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.profile.data.RealProfileRepository
import com.famex.feature.profile.data.remote.ProfileApiService
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.data.RealTransactionRepository
import com.famex.feature.transaction.data.remote.TransactionApiService
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario
import com.famex.core.network.AuthTokenStorage
import com.famex.core.network.BackendConfigStorage
import com.famex.core.network.HouseholdRequestContextProvider
import com.famex.core.network.createHttpClient
import com.famex.core.persistence.SettingsStorage
import com.famex.core.persistence.createSettingsStorage
import com.famex.core.session.CurrentHouseholdHolder

// Manual composition root (no Koin yet) — repos are interface-first so swapping these
// Fake* implementations for real Ktor-backed ones later only touches this file.
//
// `scenario` is now unused: every repository is real. It's kept as a constructor parameter
// (rather than removed) since the Fake*Repository classes it used to seed are still in the
// codebase as reference/offline-preview implementations — see e.g. FakeProfileRepository's class
// doc — and App.kt still threads ActiveDummyScenario through here. Swapping any Real*Repository
// back to its Fake counterpart for local preview only touches this file.
class AppContainer(scenario: DummyScenario = DummyScenario.HealthyMidMonth) {
    // The shared network client (core/network/HttpClientFactory.kt) backs every repository below
    // plus the Backend Configuration "Server Reachable" ping.
    private val httpClient = createHttpClient()

    // Backs AuthSession + BackendConfig persistence — the app's only local storage so far,
    // everything else is still in-memory Fake*Repository state.
    private val settingsStorage: SettingsStorage = createSettingsStorage()

    // Access-token storage for the networking layer (core/network/AuthTokenStorage.kt) — read
    // and written by RealAuthRepository, read by every other Real*Repository.
    val authTokenStorage: AuthTokenStorage = AuthTokenStorage(settingsStorage)

    // BackendConfig persistence, shared by every Real*Repository that builds a request URL.
    private val backendConfigStorage: BackendConfigStorage = BackendConfigStorage(settingsStorage)

    // "Which household is the signed-in user in" — see core/session/CurrentHouseholdHolder.kt.
    // App.kt sets this whenever the session changes; Real*Repository implementations whose
    // domain interface doesn't take a household id (Category/TransactionRepository today) read
    // it here, via the bundling provider below.
    val currentHouseholdHolder: CurrentHouseholdHolder = CurrentHouseholdHolder()

    // Bundles token + BackendConfig + household id for every authenticated
    // /households/{id}/... call — see core/network/HouseholdRequestContext.kt.
    private val householdRequestContextProvider = HouseholdRequestContextProvider(
        tokenStorage = authTokenStorage,
        backendConfigStorage = backendConfigStorage,
        householdHolder = currentHouseholdHolder
    )

    val authRepository: AuthRepository = RealAuthRepository(
        httpClient = httpClient,
        api = AuthApiService(httpClient),
        settingsStorage = settingsStorage,
        tokenStorage = authTokenStorage,
        backendConfigStorage = backendConfigStorage
    )
    val categoryRepository: CategoryRepository = RealCategoryRepository(
        api = CategoryApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    val transactionRepository: TransactionRepository = RealTransactionRepository(
        api = TransactionApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    val dashboardRepository: DashboardRepository = RealDashboardRepository(
        api = DashboardApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    val profileRepository: ProfileRepository = RealProfileRepository(
        api = ProfileApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer provides AppContainer())")
}
