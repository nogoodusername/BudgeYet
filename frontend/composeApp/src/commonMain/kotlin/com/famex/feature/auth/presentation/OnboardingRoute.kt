package com.famex.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.famex.core.model.AuthSession
import com.famex.core.model.Household
import com.famex.core.model.User
import com.famex.core.navigation.AuthTab
import com.famex.core.navigation.BackHandler
import com.famex.core.navigation.OnboardingNavController
import com.famex.core.navigation.OnboardingScreen
import com.famex.theme.LocalFamExTypography

/**
 * Owns the onboarding funnel's own back stack (separate from AppNavController — no bottom nav,
 * torn down entirely once a household is ready). Wires together the Stitch screens: Welcome,
 * Sign In/Sign Up (Auth), Backend Configuration, PIN Sent (forgot-PIN only — signup no longer
 * routes here since it logs straight in with the PIN the user just chose), Forgot PIN,
 * Household Choice, Create Household, Budget Goal, Configure Categories, Join Household.
 */
@Composable
fun OnboardingRoute(
    onOnboardingComplete: (AuthSession) -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = remember { OnboardingNavController() }
    // Set whenever AuthEvent.LoggedIn fires (from either the Log In tab or a fresh Sign Up,
    // which auto-logs in) — Create/Join Household only return a Household, so this is what lets
    // us assemble the final AuthSession(user, household) once one of those completes.
    var authedUser by remember { mutableStateOf<User?>(null) }
    val current = nav.current

    // Without this, the system back button/gesture bypasses our hand-rolled back stack
    // entirely and finishes the Activity — e.g. from Auth it would exit the app instead of
    // returning to Welcome. Disabled at Welcome (the root) so back there falls through to the
    // OS default, same as canGoBack gates the in-screen back arrow below.
    BackHandler(enabled = nav.canGoBack) { nav.back() }

    fun completeWithHousehold(household: Household) {
        val user = authedUser ?: return
        onOnboardingComplete(AuthSession(user = user, household = household))
    }

    // Unlike MainAppShell (Scaffold, which paints its own background), this root has no
    // Surface/Scaffold under it — without an explicit fill the canvas stays whatever the
    // platform default is (white on iOS) regardless of theme, so dark-theme text (chosen
    // for a dark background) went near-invisible here specifically.
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (current.showsSharedTopBar()) {
            OnboardingTopBar(title = current.topBarTitle(), canGoBack = nav.canGoBack, onBack = { nav.back() })
        }

        when (val screen = current) {
            OnboardingScreen.Welcome -> WelcomeScreen(
                onGetStarted = { nav.navigate(OnboardingScreen.Auth(AuthTab.SIGN_UP)) },
                onLogIn = { nav.navigate(OnboardingScreen.Auth(AuthTab.LOG_IN)) }
            )

            is OnboardingScreen.Auth -> AuthRoute(
                initialTab = screen.initialTab,
                onLoggedIn = { session ->
                    authedUser = session.user
                    val household = session.household
                    if (household != null) {
                        onOnboardingComplete(session)
                    } else {
                        nav.navigate(OnboardingScreen.HouseholdChoice(session.user.email))
                    }
                },
                onForgotPin = { nav.navigate(OnboardingScreen.ForgotPin) },
                onOpenBackendConfig = { nav.navigate(OnboardingScreen.BackendConfig) }
            )

            OnboardingScreen.BackendConfig -> BackendConfigRoute(onSaved = { nav.back() })

            is OnboardingScreen.PinSent -> PinSentRoute(
                email = screen.email,
                onGoToSignIn = { nav.resetToAuth(AuthTab.LOG_IN) }
            )

            OnboardingScreen.ForgotPin -> ForgotPinRoute(
                onSubmitted = { email -> nav.navigate(OnboardingScreen.PinSent(email)) },
                onBackToSignIn = { nav.resetToAuth(AuthTab.LOG_IN) }
            )

            is OnboardingScreen.HouseholdChoice -> HouseholdChoiceScreen(
                onCreateHousehold = { nav.navigate(OnboardingScreen.CreateHousehold(screen.email)) },
                onJoinHousehold = { nav.navigate(OnboardingScreen.JoinHousehold(screen.email)) }
            )

            is OnboardingScreen.CreateHousehold -> CreateHouseholdRoute(
                email = screen.email,
                onCreated = { household -> nav.navigate(OnboardingScreen.BudgetGoal(household)) }
            )

            is OnboardingScreen.BudgetGoal -> BudgetGoalRoute(
                household = screen.household,
                onSaved = { household, monthlyGoalAmount ->
                    nav.navigate(OnboardingScreen.ConfigureCategories(household, monthlyGoalAmount))
                },
                onSkipped = { household -> completeWithHousehold(household) }
            )

            is OnboardingScreen.ConfigureCategories -> ConfigureCategoriesRoute(
                household = screen.household,
                monthlyGoalAmount = screen.monthlyGoalAmount,
                onFinished = { household -> completeWithHousehold(household) }
            )

            // Joining attaches to a household someone else already budgeted/categorized, so it
            // skips Budget Goal/Configure Categories entirely and completes onboarding directly.
            is OnboardingScreen.JoinHousehold -> JoinHouseholdRoute(
                email = screen.email,
                onJoined = { household -> completeWithHousehold(household) }
            )
        }
    }
}

// Welcome/Auth/HouseholdChoice render their own header inline (matching the Stitch mockups —
// no back arrow on any of the three); everything else gets the shared bar below.
private fun OnboardingScreen.showsSharedTopBar(): Boolean = when (this) {
    OnboardingScreen.Welcome, is OnboardingScreen.Auth, is OnboardingScreen.HouseholdChoice -> false
    else -> true
}

private fun OnboardingScreen.topBarTitle(): String = when (this) {
    OnboardingScreen.BackendConfig -> "Backend Configuration"
    is OnboardingScreen.BudgetGoal -> "Set Up Budget"
    is OnboardingScreen.ConfigureCategories -> "Configure Categories"
    else -> "Fam-Ex"
}

@Composable
private fun OnboardingTopBar(title: String, canGoBack: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
        Text(text = title, style = LocalFamExTypography.current.headlineSm, color = MaterialTheme.colorScheme.onSurface)
    }
}
