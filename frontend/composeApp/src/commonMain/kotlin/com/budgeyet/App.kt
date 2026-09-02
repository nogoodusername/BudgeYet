package com.budgeyet

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.budgeyet.core.di.AppContainer
import com.budgeyet.core.di.LocalAppContainer
import com.budgeyet.core.model.AuthSession
import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.Household
import com.budgeyet.core.navigation.AppNavController
import com.budgeyet.core.navigation.BackHandler
import com.budgeyet.core.navigation.Screen
import com.budgeyet.core.offline.SyncEvent
import com.budgeyet.core.ui.BottomNavTab
import com.budgeyet.core.ui.BudgeYetBottomNavBar
import com.budgeyet.core.ui.dismissKeyboardOnTap
import com.budgeyet.core.ui.keyboardAwarePadding
import com.budgeyet.feature.auth.presentation.BudgetGoalRoute
import com.budgeyet.feature.auth.presentation.HouseholdSetupRoute
import com.budgeyet.feature.auth.presentation.OnboardingRoute
import com.budgeyet.feature.category.presentation.AddCategoryRoute
import com.budgeyet.feature.category.presentation.CategoryDetailRoute
import com.budgeyet.feature.category.presentation.CategoryRoute
import com.budgeyet.feature.dashboard.presentation.DashboardRoute
import com.budgeyet.feature.profile.presentation.HouseholdMembersRoute
import com.budgeyet.feature.profile.presentation.InviteMemberRoute
import com.budgeyet.feature.profile.presentation.ProfileRoute
import com.budgeyet.feature.transaction.presentation.AddTransactionRoute
import com.budgeyet.feature.transaction.presentation.EditTransactionRoute
import com.budgeyet.feature.transaction.presentation.HistoryRoute
import com.budgeyet.feature.transaction.presentation.TransactionDetailRoute
import com.budgeyet.fixtures.DummyScenario
import com.budgeyet.theme.BrandAmber
import com.budgeyet.theme.BudgeYetTheme

// Code-level dummy-data switch (no in-app dev switcher by design) — change and rebuild to
// preview other fixtures/DummyScenario.kt cases while real networking isn't wired up.
private val ActiveDummyScenario = DummyScenario.HealthyMidMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val container = remember { AppContainer(scenario = ActiveDummyScenario) }
    val scope = rememberCoroutineScope()
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
        container.currentHouseholdHolder.userId = newSession?.user?.id
    }

    val darkTheme = when (session?.user?.displayMode) {
        DisplayMode.DARK -> true
        DisplayMode.LIGHT -> false
        DisplayMode.SYSTEM, null -> isSystemInDarkTheme()
    }

    val onDisplayModeChanged: (DisplayMode) -> Unit = { mode ->
        session?.let { currentSession ->
            val updatedSession = currentSession.copy(
                user = currentSession.user.copy(displayMode = mode)
            )
            updateSession(updatedSession)
            scope.launch { container.authRepository.persistSession(updatedSession) }
        }
    }

    BudgeYetTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalAppContainer provides container) {
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
                    // Offline sync: while signed in, watch connectivity and drain the pending
                    // write queue on every offline→online transition. Starts as "online" so a cold
                    // start with a leftover queue from a previous session syncs immediately.
                    val connectivityFlow = remember(container) { container.connectivityObserver.observe() }
                    val isOnline by connectivityFlow.collectAsState(initial = true)
                    val pendingSyncCount by container.syncManager.pendingCount.collectAsState()

                    LaunchedEffect(isOnline) {
                        if (isOnline) container.syncManager.processQueue()
                    }

                    // Server-rejected access token (HTTP 401 — invalid/expired, no refresh token
                    // to renew it with, see core/network/AuthTokenStorage.kt): sign out. Without
                    // this the user is stranded on the failing screen's error state with only a
                    // Retry button ("Invalid or expired access token" with nowhere to go).
                    LaunchedEffect(container.sessionExpiryNotifier) {
                        container.sessionExpiryNotifier.events.collect {
                            updateSession(null)
                            scope.launch { container.authRepository.clearPersistedSession() }
                        }
                    }

                    // Pick up a queue left behind by a previous session (e.g. the app was killed
                    // while offline) so the badge shows immediately instead of only after a sync.
                    LaunchedEffect(container.syncManager) {
                        container.syncManager.refreshPendingCount()
                    }

                    val onSignOut: () -> Unit = {
                        updateSession(null)
                        scope.launch { container.authRepository.clearPersistedSession() }
                    }
                    val activeHousehold = currentSession.household

                    if (activeHousehold == null) {
                        // Signed in with no household — either a fresh login that hasn't picked
                        // one yet (see AuthSession's doc comment) or a solo Owner who just
                        // deleted theirs. Let them create or join one without re-authenticating;
                        // the access token is still valid.
                        HouseholdSetupRoute(
                            email = currentSession.user.email,
                            onHouseholdReady = { household ->
                                val updated = currentSession.copy(household = household)
                                updateSession(updated)
                                scope.launch { container.authRepository.persistSession(updated) }
                            },
                            onSignOut = onSignOut
                        )
                    } else {
                        MainAppShell(
                            household = activeHousehold,
                            onSignOut = onSignOut,
                            pendingSyncCount = pendingSyncCount,
                            onDisplayModeChanged = onDisplayModeChanged,
                            onHouseholdDeleted = {
                                val updated = currentSession.copy(household = null)
                                updateSession(updated)
                                scope.launch { container.authRepository.persistSession(updated) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppShell(
    household: Household,
    onSignOut: () -> Unit,
    pendingSyncCount: Int,
    onDisplayModeChanged: (DisplayMode) -> Unit = {},
    onHouseholdDeleted: () -> Unit = {}
) {
    val container = LocalAppContainer.current
    val navController = remember { AppNavController() }
    val current = navController.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Same rationale as OnboardingRoute's BackHandler — without it, system back skips our
    // back stack and exits the app from any pushed screen (detail views, add/edit forms)
    // instead of returning to the previous one. Disabled at a root tab (canGoBack == false)
    // so back there falls through to the OS default (e.g. backgrounding the app).
    BackHandler(enabled = navController.canGoBack) { navController.back() }

    // Offline-sync rejections (server-wins conflicts, permanent 4xx) surface as toasts so the
    // user knows an offline change wasn't applied instead of silently losing it.
    LaunchedEffect(container.syncManager) {
        container.syncManager.events.collect { event ->
            when (event) {
                is SyncEvent.Rejected -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        // "N changes waiting to sync" — mirrors the amber pending-state token used
                        // across the app (BrandAmber = 75-99%, here repurposed as "queued, not yet
                        // confirmed"); disappears once SyncManager drains the queue.
                        if (pendingSyncCount > 0) {
                            Row(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(BrandAmber, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$pendingSyncCount pending",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandAmber
                                )
                            }
                        }
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
                BudgeYetBottomNavBar(
                    selectedTab = current.toBottomNavTab(),
                    // Hidden on the add/edit transaction forms and Add Category — it would
                    // float on top of those screens' own Save/Add button.
                    showAddButton = current != Screen.AddTransaction && current !is Screen.EditTransaction &&
                        current != Screen.AddCategory && current != Screen.BudgetSetup,
                    onDashboard = { navController.switchTab(Screen.Dashboard) },
                    onCategories = { navController.switchTab(Screen.Categories) },
                    onAdd = { navController.navigate(Screen.AddTransaction) },
                    onHistory = { navController.switchTab(Screen.History) },
                    onProfile = { navController.switchTab(Screen.Profile) }
                )
            }
        ) { paddingValues ->
            // keyboardAwarePadding() extends the content area upward by the keyboard height so a
            // scrollable screen (e.g. Category Limits) can bring fields/buttons above the keyboard
            // instead of leaving them covered; dismissKeyboardOnTap() gives iOS (no hardware Back)
            // a way to close the keyboard by tapping empty space.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .dismissKeyboardOnTap()
                    .keyboardAwarePadding()
            ) {
                when (val screen = current) {
                    Screen.Dashboard -> DashboardRoute(
                        onNavigateToCategoryDetail = { navController.navigate(Screen.CategoryDetail(it)) },
                        onNavigateToHistory = { navController.switchTab(Screen.History) },
                        onNavigateToSetUpBudget = { navController.navigate(Screen.BudgetSetup) },
                        onNavigateToAddCategory = { navController.navigate(Screen.AddCategory) }
                    )
                    Screen.BudgetSetup -> BudgetGoalRoute(
                        household = household,
                        isOnboarding = false,
                        // Route straight to Categories (not Dashboard) — a fresh budget has no
                        // categories yet, and Categories' empty state is where users can add one.
                        onSaved = { _, _ -> navController.switchTab(Screen.Categories) },
                        onSkipped = { navController.back() }
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
                        onSignOut = onSignOut,
                        onDisplayModeChanged = onDisplayModeChanged
                    )
                    Screen.HouseholdMembers -> HouseholdMembersRoute(
                        onNavigateToInvite = { navController.navigate(Screen.InviteMember) },
                        onHouseholdDeleted = onHouseholdDeleted
                    )
                    Screen.InviteMember -> InviteMemberRoute(onInvited = { navController.back() })
                }
            }
        }
}

private fun Screen.title(): String = when (this) {
    Screen.Dashboard -> "Dashboard"
    Screen.Categories -> "Category Limits"
    is Screen.CategoryDetail -> "Category Detail"
    Screen.AddCategory -> "Add Category"
    Screen.BudgetSetup -> "Set Up Budget"
    Screen.History -> "Transaction History"
    is Screen.TransactionDetail -> "Transaction Detail"
    is Screen.EditTransaction -> "Edit Transaction"
    Screen.AddTransaction -> "Log Expense"
    Screen.Profile -> "Profile & Settings"
    Screen.HouseholdMembers -> "Household Members"
    Screen.InviteMember -> "Invite Member"
}

private fun Screen.toBottomNavTab(): BottomNavTab = when (this) {
    Screen.Dashboard, Screen.BudgetSetup -> BottomNavTab.Dashboard
    Screen.Categories, is Screen.CategoryDetail, Screen.AddCategory -> BottomNavTab.Categories
    Screen.History, is Screen.TransactionDetail, is Screen.EditTransaction -> BottomNavTab.History
    Screen.Profile, Screen.HouseholdMembers, Screen.InviteMember -> BottomNavTab.Profile
    Screen.AddTransaction -> BottomNavTab.None
}
