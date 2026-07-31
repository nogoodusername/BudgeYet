package com.famex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.famex.core.di.AppContainer
import com.famex.core.di.LocalAppContainer
import com.famex.core.navigation.AppNavController
import com.famex.core.navigation.Screen
import com.famex.feature.category.presentation.CategoryDetailRoute
import com.famex.feature.category.presentation.CategoryRoute
import com.famex.feature.dashboard.presentation.DashboardRoute
import com.famex.feature.profile.presentation.ProfileRoute
import com.famex.feature.transaction.presentation.AddTransactionPlaceholderScreen
import com.famex.feature.transaction.presentation.HistoryRoute
import com.famex.feature.transaction.presentation.TransactionDetailRoute
import com.famex.fixtures.DummyScenario
import com.famex.theme.BrandTeal
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
                    NavigationBar {
                        NavigationBarItem(
                            selected = current is Screen.Dashboard,
                            onClick = { navController.switchTab(Screen.Dashboard) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = current is Screen.Categories,
                            onClick = { navController.switchTab(Screen.Categories) },
                            icon = { Icon(Icons.Default.Menu, contentDescription = "Categories") },
                            label = { Text("Categories") }
                        )
                        NavigationBarItem(
                            selected = current is Screen.History,
                            onClick = { navController.switchTab(Screen.History) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "History") },
                            label = { Text("History") }
                        )
                        NavigationBarItem(
                            selected = current is Screen.Profile,
                            onClick = { navController.switchTab(Screen.Profile) },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.AddTransaction) },
                        containerColor = BrandTeal,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                    }
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (val screen = current) {
                        Screen.Dashboard -> DashboardRoute(
                            onNavigateToCategoryDetail = { navController.navigate(Screen.CategoryDetail(it)) },
                            onNavigateToHistory = { navController.switchTab(Screen.History) }
                        )
                        Screen.Categories -> CategoryRoute(
                            onCategoryClick = { navController.navigate(Screen.CategoryDetail(it)) }
                        )
                        is Screen.CategoryDetail -> CategoryDetailRoute(categoryId = screen.categoryId)
                        Screen.History -> HistoryRoute(
                            onTransactionClick = { navController.navigate(Screen.TransactionDetail(it)) }
                        )
                        is Screen.TransactionDetail -> TransactionDetailRoute(transactionId = screen.transactionId)
                        Screen.AddTransaction -> AddTransactionPlaceholderScreen()
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
    is Screen.TransactionDetail -> "Transaction Detail"
    Screen.AddTransaction -> "Add Transaction"
    Screen.Profile -> "Profile"
}
