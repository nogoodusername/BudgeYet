package com.famex.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.famex.feature.auth.data.RealAuthRepository
import com.famex.feature.auth.data.remote.AuthApiService
import com.famex.feature.auth.domain.AuthRepository
import com.famex.feature.category.data.OfflineFirstCategoryRepository
import com.famex.feature.category.data.RealCategoryRepository
import com.famex.feature.category.data.remote.CategoryApiService
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.dashboard.data.OfflineFirstDashboardRepository
import com.famex.feature.dashboard.data.RealDashboardRepository
import com.famex.feature.dashboard.data.remote.DashboardApiService
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.profile.data.OfflineFirstProfileRepository
import com.famex.feature.profile.data.RealProfileRepository
import com.famex.feature.profile.data.remote.ProfileApiService
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.data.OfflineFirstTransactionRepository
import com.famex.feature.transaction.data.RealTransactionRepository
import com.famex.feature.transaction.data.remote.TransactionApiService
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario
import com.famex.core.cache.LocalCacheStore
import com.famex.core.cache.LocalFileStorage
import com.famex.core.cache.createLocalFileStorage
import com.famex.core.network.AuthTokenStorage
import com.famex.core.network.BackendConfigStorage
import com.famex.core.network.HouseholdRequestContextProvider
import com.famex.core.network.createHttpClient
import com.famex.core.offline.OfflineQueue
import com.famex.core.offline.SyncManager
import com.famex.core.persistence.SettingsStorage
import com.famex.core.persistence.createSettingsStorage
import com.famex.core.session.CurrentHouseholdHolder
import com.famex.core.util.ConnectivityObserver
import com.famex.core.util.createConnectivityObserver

// Manual composition root (no Koin yet) — repos are interface-first so swapping these
// Fake* implementations for real Ktor-backed ones later only touches this file.
//
// `scenario` is now unused: every repository is real (and wrapped for offline support). It's kept
// as a constructor parameter (rather than removed) since the Fake*Repository classes it used to
// seed are still in the codebase as reference implementations — see e.g. FakeProfileRepository's
// class doc — and App.kt still threads ActiveDummyScenario through here. Swapping any
// OfflineFirst*Repository back to its Real/Fake counterpart for local preview only touches this
// file.
//
// Offline support (see AGENTS.md "offline support"): every feature repository exposed here is an
// OfflineFirst* wrapper over its Real* counterpart. Reads are network-first with cache fallback;
// transaction writes queue in the OfflineQueue and drain via SyncManager on reconnect (driven by
// App.kt observing connectivityObserver). Everything else surfaces network errors inline.
class AppContainer(scenario: DummyScenario = DummyScenario.HealthyMidMonth) {
    // The shared network client (core/network/HttpClientFactory.kt) backs every repository below
    // plus the Backend Configuration "Server Reachable" ping.
    private val httpClient = createHttpClient()

    // Backs AuthSession + BackendConfig persistence — the app's smallest local storage (prefs).
    private val settingsStorage: SettingsStorage = createSettingsStorage()

    // File-backed storage for the offline cache + write queue (core/cache + core/offline) —
    // larger JSON blobs (transaction/category lists, the queue) that don't belong in prefs.
    private val localFileStorage: LocalFileStorage = createLocalFileStorage()
    private val localCacheStore = LocalCacheStore(localFileStorage)
    private val offlineQueue = OfflineQueue(localFileStorage)

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

    // Network-state observer + offline queue drainer. App.kt observes connectivityObserver and
    // calls syncManager.processQueue() on every offline→online transition; pendingCount feeds the
    // "N changes waiting to sync" top-bar badge and events feed rejected-change toasts.
    val connectivityObserver: ConnectivityObserver = createConnectivityObserver()

    // Auth is deliberately NOT offline-wrapped: login/signup/forgot-PIN require a live server.
    val authRepository: AuthRepository = RealAuthRepository(
        httpClient = httpClient,
        api = AuthApiService(httpClient),
        settingsStorage = settingsStorage,
        tokenStorage = authTokenStorage,
        backendConfigStorage = backendConfigStorage
    )

    // Real, unwrapped repos — the network truth SyncManager replays the queue against, and the
    // delegate each OfflineFirst*Repository wraps. Constructed first so the sync manager can use
    // the real transaction repo (never the wrapper, which would re-queue instead of replaying).
    private val realCategoryRepository = RealCategoryRepository(
        api = CategoryApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    private val realTransactionRepository = RealTransactionRepository(
        api = TransactionApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    private val realDashboardRepository = RealDashboardRepository(
        api = DashboardApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )
    private val realProfileRepository = RealProfileRepository(
        api = ProfileApiService(httpClient),
        contextProvider = householdRequestContextProvider
    )

    val syncManager: SyncManager = SyncManager(
        transactionRepository = realTransactionRepository,
        cacheStore = localCacheStore,
        queue = offlineQueue
    )

    val categoryRepository: CategoryRepository = OfflineFirstCategoryRepository(
        delegate = realCategoryRepository,
        cacheStore = localCacheStore
    )
    val transactionRepository: TransactionRepository = OfflineFirstTransactionRepository(
        delegate = realTransactionRepository,
        cacheStore = localCacheStore,
        queue = offlineQueue,
        syncManager = syncManager
    )
    val dashboardRepository: DashboardRepository = OfflineFirstDashboardRepository(
        delegate = realDashboardRepository,
        cacheStore = localCacheStore
    )
    val profileRepository: ProfileRepository = OfflineFirstProfileRepository(
        delegate = realProfileRepository,
        cacheStore = localCacheStore
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer provides AppContainer())")
}
