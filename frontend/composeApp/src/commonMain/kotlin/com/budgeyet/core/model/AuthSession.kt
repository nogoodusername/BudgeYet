package com.budgeyet.core.model

import kotlinx.serialization.Serializable

// household == null means the signed-in user hasn't created or joined one yet — the app should
// route to the Household Choice / Create / Join onboarding screens rather than the main shell.
// @Serializable so AuthRepository.persistSession can store it as JSON (see SettingsStorage) —
// this is what lets a cold start skip onboarding instead of resetting to signed-out every time.
@Serializable
data class AuthSession(val user: User, val household: Household?)
