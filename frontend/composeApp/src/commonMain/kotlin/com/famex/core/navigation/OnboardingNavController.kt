package com.famex.core.navigation

import androidx.compose.runtime.mutableStateListOf

// Mirrors AppNavController's hand-rolled sealed-Screen + back-stack approach — kept as a
// separate controller/stack from the main app's since onboarding has its own linear flow and
// chrome (no bottom nav) and is torn down entirely once a household is ready.
class OnboardingNavController(startScreen: OnboardingScreen = OnboardingScreen.Welcome) {
    private val backStack = mutableStateListOf(startScreen)

    val current: OnboardingScreen get() = backStack.last()
    val canGoBack: Boolean get() = backStack.size > 1

    fun navigate(screen: OnboardingScreen) {
        backStack.add(screen)
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    // Used by "Go to Sign In" (from PIN Sent) / "Back to Sign In" (from Forgot PIN) so those
    // intermediate screens don't linger on the back stack once the user is back at Auth.
    fun resetToAuth(initialTab: AuthTab = AuthTab.LOG_IN) {
        backStack.clear()
        backStack.add(OnboardingScreen.Auth(initialTab))
    }
}
