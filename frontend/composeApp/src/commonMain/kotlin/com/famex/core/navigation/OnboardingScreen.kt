package com.famex.core.navigation

enum class AuthTab { LOG_IN, SIGN_UP }

sealed interface OnboardingScreen {
    data object Welcome : OnboardingScreen
    data class Auth(val initialTab: AuthTab = AuthTab.LOG_IN) : OnboardingScreen
    data object BackendConfig : OnboardingScreen
    // Only reachable from Forgot PIN — signup no longer needs an email round-trip since the
    // user already knows the PIN they just chose (AuthController.onSignUp logs straight in).
    data class PinSent(val email: String) : OnboardingScreen
    data object ForgotPin : OnboardingScreen

    // Carry the just-authenticated user's email forward rather than tracking "pending session"
    // as shared mutable state — Create/Join Household need it to attach the new household to
    // the right account (see AuthRepository.createHousehold/joinHousehold).
    data class HouseholdChoice(val email: String) : OnboardingScreen
    data class CreateHousehold(val email: String) : OnboardingScreen
    data class JoinHousehold(val email: String) : OnboardingScreen
}
