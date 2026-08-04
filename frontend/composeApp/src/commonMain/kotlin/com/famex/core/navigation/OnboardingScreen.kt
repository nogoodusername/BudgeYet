package com.famex.core.navigation

enum class AuthTab { LOG_IN, SIGN_UP }
enum class PinSentContext { SIGN_UP, FORGOT_PIN }

sealed interface OnboardingScreen {
    data object Welcome : OnboardingScreen
    data class Auth(val initialTab: AuthTab = AuthTab.LOG_IN) : OnboardingScreen
    data object BackendConfig : OnboardingScreen
    data class PinSent(val email: String, val context: PinSentContext) : OnboardingScreen
    data object ForgotPin : OnboardingScreen

    // Carry the just-authenticated user's email forward rather than tracking "pending session"
    // as shared mutable state — Create/Join Household need it to attach the new household to
    // the right account (see AuthRepository.createHousehold/joinHousehold).
    data class HouseholdChoice(val email: String) : OnboardingScreen
    data class CreateHousehold(val email: String) : OnboardingScreen
    data class JoinHousehold(val email: String) : OnboardingScreen
}
