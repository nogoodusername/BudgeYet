package com.famex.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.famex.feature.category.data.FakeCategoryRepository
import com.famex.feature.category.domain.CategoryRepository
import com.famex.feature.dashboard.data.FakeDashboardRepository
import com.famex.feature.dashboard.domain.DashboardRepository
import com.famex.feature.profile.data.FakeProfileRepository
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.feature.transaction.data.FakeTransactionRepository
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario

// Manual composition root (no Koin yet) — repos are interface-first so swapping these
// Fake* implementations for real Ktor-backed ones later only touches this file.
class AppContainer(scenario: DummyScenario = DummyScenario.HealthyMidMonth) {
    val dashboardRepository: DashboardRepository = FakeDashboardRepository(scenario)
    val categoryRepository: CategoryRepository = FakeCategoryRepository(scenario)
    val transactionRepository: TransactionRepository = FakeTransactionRepository(scenario)
    val profileRepository: ProfileRepository = FakeProfileRepository(scenario)
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — wrap content in CompositionLocalProvider(LocalAppContainer provides AppContainer())")
}
