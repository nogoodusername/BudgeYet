package com.famex.feature.auth.domain

import com.famex.core.model.AuthSession
import com.famex.core.model.BackendConfig
import com.famex.core.model.Household

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

    suspend fun getBackendConfig(): BackendConfig
    suspend fun setBackendConfig(config: BackendConfig)
}
