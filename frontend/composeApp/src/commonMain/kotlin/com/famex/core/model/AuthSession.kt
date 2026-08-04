package com.famex.core.model

// household == null means the signed-in user hasn't created or joined one yet — the app should
// route to the Household Choice / Create / Join onboarding screens rather than the main shell.
data class AuthSession(val user: User, val household: Household?)
