package com.famex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import com.famex.core.di.AppContainer
import com.famex.core.di.LocalAppContainer
import com.famex.core.model.AuthSession
import com.famex.core.navigation.AppNavController
import com.famex.core.navigation.BackHandler
import com.famex.core.navigation.Screen
import com.famex.core.ui.BottomNavTab
import com.famex.core.ui.FamExBottomNavBar
import com.famex.feature.auth.presentation.OnboardingRoute
import com.famex.feature.category.presentation.AddCategoryRoute
import com.famex.feature.category.presentation.CategoryDetailRoute
import com.famex.feature.category.presentation.CategoryRoute
import com.famex.feature.dashboard.presentation.DashboardRoute
import com.famex.feature.profile.presentation.HouseholdMembersRoute
import com.famex.feature.profile.presentation.InviteMemberRoute
import com.famex.feature.profile.presentation.ProfileRoute
import com.famex.feature.transaction.presentation.AddTransactionRoute
import com.famex.feature.transaction.presentation.EditTransactionRoute
import com.famex.feature.transaction.presentation.HistoryRoute
import com.famex.feature.transaction.presentation.TransactionDetailRoute
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
        val scope = rememberCoroutineScope()
        CompositionLocalProvider(LocalAppContainer provides container) {
            var session by remember { mutableStateOf<AuthSession?>(null) }
            // Gates the very first frame on a SettingsStorage read (see
            // AuthRepository.getPersistedSession) so a signed-in cold start renders straight
            // into MainAppShell instead of flashing OnboardingRoute first.
            var isRestoringSession by remember { mutableStateOf(true) }

            // Keeps CurrentHouseholdHolder (core/session/) in step with the session — Real
            // repositories whose interface doesn't take a household id (CategoryRepository
            // today, more to follow) read it from there. MainAppShell only ever renders once
            // newSession.household is non-null, so the holder is always populated by the time
            // any of those repositories can actually be called.
            fun updateSession(newSession: AuthSession?) {
                session = newSession
                container.currentHouseholdHolder.householdId = newSession?.household?.id
            }

            LaunchedEffect(container) {
                updateSession(container.authRepository.getPersistedSession())
                isRestoringSession = false
            }

            if (isRestoringSession) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            } else {
                val currentSession = session

                if (currentSession == null) {
                    OnboardingRoute(
                        onOnboardingComplete = { newSession ->
                            updateSession(newSession)
                            scope.launch { container.authRepository.persistSession(newSession) }
                        }
                    )
                } else {
                    MainAppShell(
                        onSignOut = {
                            updateSession(null)
                            scope.launch { container.authRepository.clearPersistedSession() }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(onSignOut: () -> Unit) {
    val navController = remember { AppNavController() }
    val current = navController.current

    // Same rationale as OnboardingRoute's BackHandler — without it, system back skips our
    // back stack and exits the app from any pushed screen (detail views, add/edit forms)
    // instead of returning to the previous one. Disabled at a root tab (canGoBack == false)
    // so back there falls through to the OS default (e.g. backgrounding the app).
    BackHandler(enabled = navController.canGoBack) { navController.back() }

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
                    actions = {
                        if (current is Screen.TransactionDetail) {
                            TextButton(onClick = { navController.navigate(Screen.EditTransaction(current.transactionId)) }) {
                                Text(text = "Edit", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            bottomBar = {
                FamExBottomNavBar(
                    selectedTab = current.toBottomNavTab(),
                    // Hidden on the add/edit transaction forms and Add Category — it would
                    // float on top of those screens' own Save/Add button.
                    showAddButton = current != Screen.AddTransaction && current !is Screen.EditTransaction &&
                        current != Screen.AddCategory,
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
                    Screen.Categories -> CategoryRoute(
                        onNavigateToCategoryDetail = { navController.navigate(Screen.CategoryDetail(it)) },
                        onNavigateToAddCategory = { navController.navigate(Screen.AddCategory) }
                    )
                    is Screen.CategoryDetail -> CategoryDetailRoute(
                        categoryId = screen.categoryId,
                        onDeleted = { navController.switchTab(Screen.Categories) }
                    )
                    Screen.AddCategory -> AddCategoryRoute(onSaved = { navController.back() })
                    Screen.History -> HistoryRoute(
                        onTransactionClick = { navController.navigate(Screen.TransactionDetail(it)) },
                        onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction) }
                    )
                    is Screen.TransactionDetail -> TransactionDetailRoute(
                        transactionId = screen.transactionId,
                        onEdit = { navController.navigate(Screen.EditTransaction(it)) },
                        onDeleted = { navController.switchTab(Screen.History) }
                    )
                    is Screen.EditTransaction -> EditTransactionRoute(
                        transactionId = screen.transactionId,
                        onSaved = { navController.back() },
                        onDeleted = { navController.switchTab(Screen.History) }
                    )
                    Screen.AddTransaction -> AddTransactionRoute(onSaved = { navController.back() })
                    Screen.Profile -> ProfileRoute(
                        onNavigateToManageMembers = { navController.navigate(Screen.HouseholdMembers) },
                        onSignOut = onSignOut
                    )
                    Screen.HouseholdMembers -> HouseholdMembersRoute(
                        onNavigateToInvite = { navController.navigate(Screen.InviteMember) }
                    )
                    Screen.InviteMember -> InviteMemberRoute(onInvited = { navController.back() })
                }
            }
        }
}

private fun Screen.title(): String = when (this) {
    Screen.Dashboard -> "fam-ex Dashboard"
    Screen.Categories -> "Category Limits"
    is Screen.CategoryDetail -> "Category Detail"
    Screen.AddCategory -> "Add Category"
    Screen.History -> "Transaction History"
    is Screen.TransactionDetail -> "Transaction Detail"
    is Screen.EditTransaction -> "Edit Transaction"
    Screen.AddTransaction -> "Log Expense"
    Screen.Profile -> "Profile & Settings"
    Screen.HouseholdMembers -> "Household Members"
    Screen.InviteMember -> "Invite Member"
}

private fun Screen.toBottomNavTab(): BottomNavTab = when (this) {
    Screen.Dashboard -> BottomNavTab.Dashboard
    Screen.Categories, is Screen.CategoryDetail, Screen.AddCategory -> BottomNavTab.Categories
    Screen.History, is Screen.TransactionDetail, is Screen.EditTransaction -> BottomNavTab.History
    Screen.Profile, Screen.HouseholdMembers, Screen.InviteMember -> BottomNavTab.Profile
    Screen.AddTransaction -> BottomNavTab.None
}
