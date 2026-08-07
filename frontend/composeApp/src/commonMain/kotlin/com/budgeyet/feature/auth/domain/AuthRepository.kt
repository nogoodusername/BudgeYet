package com.budgeyet.feature.auth.domain

import com.budgeyet.core.model.AuthSession
import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.model.Household

interface AuthRepository {
    // PIN is user-chosen at signup (backend: UserCreate.pin, validated ^\d{6}$) — the backend
    // no longer generates or emails one here, unlike forgot-PIN below, which still does since
    // that flow's whole point is recovering an account the user is locked out of.
    suspend fun signUp(fullName: String, nickname: String, email: String, pin: String)

    suspend fun login(email: String, pin: String): AuthSession

    // Silently no-ops for unknown emails on the real backend (can't be used to enumerate
    // accounts) — the fake implementation mirrors that by not throwing either.
    suspend fun requestPinReset(email: String)

    // Both take the just-authenticated user's email explicitly (from the AuthSession that
    // routed here) rather than tracking "current pending user" as repository-side mutable
    // state — keeps the fake repo's state machine simple and makes the caller's intent explicit.
    suspend fun createHousehold(email: String, name: String, currency: String, cycleStartDay: Int): Household
    suspend fun joinHousehold(email: String, inviteCode: String): Household

    // Onboarding-only follow-ups to createHousehold (A4 in this Stitch batch) — mirror the
    // real backend's POST /households/{id}/budgets and one-POST-per-category
    // /households/{id}/categories, but as a single call each since the fake repo has no
    // per-category network round trip to simulate.
    suspend fun setupBudget(householdId: Long, name: String, monthlyGoalAmount: Double)
    suspend fun setupCategories(householdId: Long, categories: List<CategorySetupInput>)

    suspend fun getBackendConfig(): BackendConfig
    suspend fun setBackendConfig(config: BackendConfig)

    // Real network call (not a fake-repo simulation) — hits the target server's DB-independent
    // /api/v1/ping so Backend Configuration's "Server Reachable" check reflects an actual
    // liveness probe, not a UI mock. Throws with a user-facing message on any failure
    // (unreachable host, timeout, non-2xx response); returns normally on success.
    suspend fun checkServerReachable(url: String)

    // Local session persistence (SettingsStorage-backed, see FakeAuthRepository) — lets App.kt
    // restore a signed-in session on cold start instead of always resetting to OnboardingRoute.
    // Not the same thing as real auth/session-token persistence (no such tokens exist yet,
    // there's no real backend call involved) — this just remembers the last AuthSession the UI
    // reached so the fake-repo-backed app doesn't force a fresh sign-up every launch.
    suspend fun getPersistedSession(): AuthSession?
    suspend fun persistSession(session: AuthSession)
    suspend fun clearPersistedSession()
}

data class CategorySetupInput(val name: String, val icon: String, val monthlyLimit: Double)
