package com.famex.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.famex.feature.auth.data.FakeAuthRepository
import com.famex.feature.auth.domain.AuthRepository
import com.famex.feature.category.data.FakeCategoryRepository
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.dashboard.data.FakeDashboardRepository
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.profile.data.FakeProfileRepository
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.data.FakeTransactionRepository
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

// Manual composition root (no Koin yet) — repos are interface-first so swapping these
// Fake* implementations for real Ktor-backed ones later only touches this file.
class AppContainer(scenario: DummyScenario = DummyScenario.HealthyMidMonth) {
    // The one real network client in the app so far — used only for Backend Configuration's
    // "Server Reachable" ping (see FakeAuthRepository.checkServerReachable). Everything else
    // still runs on Fake*Repository dummy data until the rest of the networking layer lands.
    private val httpClient = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 5_000
        }
    }

    val dashboardRepository: DashboardRepository = FakeDashboardRepository(scenario)
    val categoryRepository: CategoryRepository = FakeCategoryRepository(scenario)
    val transactionRepository: TransactionRepository = FakeTransactionRepository(scenario)
    val profileRepository: ProfileRepository = FakeProfileRepository(scenario)
    // Deliberately not seeded from `scenario` beyond the demo account — see FakeAuthRepository.
    val authRepository: AuthRepository = FakeAuthRepository(scenario, httpClient)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer provides AppContainer())")
}
