package com.famex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.famex.core.di.AppContainer
import com.famex.core.di.LocalAppContainer
import com.famex.core.navigation.AppNavController
import com.famex.core.navigation.Screen
import com.famex.core.ui.BottomNavTab
import com.famex.core.ui.FamExBottomNavBar
import com.famex.feature.category.presentation.CategoryDetailRoute
import com.famex.feature.category.presentation.CategoryRoute
import com.famex.feature.dashboard.presentation.DashboardRoute
import com.famex.feature.profile.presentation.ProfileRoute
import com.famex.feature.transaction.presentation.AddTransactionRoute
import com.famex.feature.transaction.presentation.EditTransactionRoute
import com.famex.feature.transaction.presentation.HistoryRoute
import com.famex.fixtures.DummyScenario
import com.famex.theme.FamExTheme

// Code-level dummy-data switch (no in-app dev switcher by design) — change and rebuild to
// preview other fixtures/DummyScenario.kt cases while real networking isn't wired up.
private val ActiveDummyScenario = DummyScenario.HealthyMidMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    FamExTheme {
        val container = remember { AppContainer(scenario = ActiveDummyScenario) }
        CompositionLocalProvider(LocalAppContainer provides container) {
            val navController = remember { AppNavController() }
            val current = navController.current

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = current.title(), fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (navController.canGoBack) {
                                IconButton(onClick = { navController.back() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                bottomBar = {
                    FamExBottomNavBar(
                        selectedTab = current.toBottomNavTab(),
                        // Hidden on the add/edit transaction forms — it would float on top of
                        // those screens' own Save button.
                        showAddButton = current != Screen.AddTransaction && current !is Screen.TransactionDetail,
                        onDashboard = { navController.switchTab(Screen.Dashboard) },
                        onCategories = { navController.switchTab(Screen.Categories) },
                        onAdd = { navController.navigate(Screen.AddTransaction) },
                        onHistory = { navController.switchTab(Screen.History) },
                        onProfile = { navController.switchTab(Screen.Profile) }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (val screen = current) {
                        Screen.Dashboard -> DashboardRoute(
                            onNavigateToCategoryDetail = { navController.navigate(Screen.CategoryDetail(it)) },
                            onNavigateToHistory = { navController.switchTab(Screen.History) },
                            onNavigateToSetUpBudget = { navController.switchTab(Screen.Categories) }
                        )
                        Screen.Categories -> CategoryRoute()
                        is Screen.CategoryDetail -> CategoryDetailRoute(categoryId = screen.categoryId)
                        Screen.History -> HistoryRoute(
                            onTransactionClick = { navController.navigate(Screen.TransactionDetail(it)) }
                        )
                        is Screen.TransactionDetail -> EditTransactionRoute(
                            transactionId = screen.transactionId,
                            onDone = { navController.back() }
                        )
                        Screen.AddTransaction -> AddTransactionRoute(onSaved = { navController.back() })
                        Screen.Profile -> ProfileRoute()
                    }
                }
            }
        }
    }
}

private fun Screen.title(): String = when (this) {
    Screen.Dashboard -> "fam-ex Dashboard"
    Screen.Categories -> "Category Limits"
    is Screen.CategoryDetail -> "Category Detail"
    Screen.History -> "Transaction History"
    is Screen.TransactionDetail -> "Edit Transaction"
    Screen.AddTransaction -> "Log Expense"
    Screen.Profile -> "Profile"
}

private fun Screen.toBottomNavTab(): BottomNavTab = when (this) {
    Screen.Dashboard -> BottomNavTab.Dashboard
    Screen.Categories, is Screen.CategoryDetail -> BottomNavTab.Categories
    Screen.History, is Screen.TransactionDetail -> BottomNavTab.History
    Screen.Profile -> BottomNavTab.Profile
    Screen.AddTransaction -> BottomNavTab.None
}
