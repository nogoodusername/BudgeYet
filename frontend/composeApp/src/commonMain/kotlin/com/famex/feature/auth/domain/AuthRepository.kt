package com.famex.feature.auth.domain

import com.famex.core.model.AuthSession
import com.famex.core.model.BackendConfig
import com.famex.core.model.Household

interface AuthRepository {
    // No PIN parameter here — the backend always generates the PIN server-side and emails it
    // (AuthService.signup), the same as forgot-PIN. UserCreate has no pin field; don't add a
    // client-chosen-PIN field to the Sign Up screen even though the Stitch mockup shows one —
    // see AGENTS.md Phase 2 notes.
    suspend fun signUp(fullName: String, nickname: String, email: String)

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
