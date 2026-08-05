package com.famex.core.network

import com.famex.core.model.BackendConfig
import com.famex.core.session.CurrentHouseholdHolder

// Everything a Real*Repository needs to call an authenticated /households/{id}/... endpoint.
// Bundled together since every such call needs all three, in order to avoid each repository
// re-deriving them independently — CategoryRepository and TransactionRepository both need this.
data class HouseholdRequestContext(
    val config: BackendConfig,
    val accessToken: String,
    val householdId: Long
)

class HouseholdRequestContextProvider(
    private val tokenStorage: AuthTokenStorage,
    private val backendConfigStorage: BackendConfigStorage,
    private val householdHolder: CurrentHouseholdHolder
) {
    suspend fun get(): HouseholdRequestContext {
        val token = tokenStorage.getToken() ?: throw AppException.AuthenticationException(
            "Your session has expired. Please sign in again."
        )
        return HouseholdRequestContext(
            config = backendConfigStorage.get(),
            accessToken = token,
            householdId = householdHolder.require()
        )
    }
}
