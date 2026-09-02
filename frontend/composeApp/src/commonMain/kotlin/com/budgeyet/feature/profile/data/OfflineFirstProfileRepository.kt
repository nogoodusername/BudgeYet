package com.budgeyet.feature.profile.data

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.Household
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.User
import com.budgeyet.core.offline.networkFirstRead
import com.budgeyet.feature.profile.domain.ProfileRepository

// Read-through-cache wrapper around RealProfileRepository: getCurrentUser/getHousehold fall back
// to the cache offline so Profile, Household Members, and Invite screens still render. Writes are
// NOT queued (only transactions sync offline — see AGENTS.md) and pass through to the real repo,
// surfacing the network error inline when offline. Every successful write re-caches the fresh
// User/Household it returns so the next offline read shows the updated state.
class OfflineFirstProfileRepository(
    private val delegate: ProfileRepository,
    private val cacheStore: LocalCacheStore
) : ProfileRepository {

    override suspend fun getCurrentUser(): User = networkFirstRead(
        networkCall = { delegate.getCurrentUser() },
        cached = { cacheStore.getCachedUser() },
        onSuccess = { cacheStore.cacheUser(it) }
    )

    override suspend fun getHousehold(): Household = networkFirstRead(
        networkCall = { delegate.getHousehold() },
        cached = { cacheStore.getCachedHousehold() },
        onSuccess = { cacheStore.cacheHousehold(it) }
    )

    override suspend fun updateProfileName(fullName: String, nickname: String): User =
        delegate.updateProfileName(fullName, nickname).also { cacheStore.cacheUser(it) }

    override suspend fun updateDisplayMode(displayMode: DisplayMode): User =
        delegate.updateDisplayMode(displayMode).also { cacheStore.cacheUser(it) }

    override suspend fun updatePushNotifications(enabled: Boolean): User =
        delegate.updatePushNotifications(enabled).also { cacheStore.cacheUser(it) }

    override suspend fun updateCurrency(currency: String): Household =
        delegate.updateCurrency(currency).also { cacheStore.cacheHousehold(it) }

    override suspend fun updateLanguage(language: String): Household =
        delegate.updateLanguage(language).also { cacheStore.cacheHousehold(it) }

    override suspend fun inviteMember(email: String): Household =
        delegate.inviteMember(email).also { cacheStore.cacheHousehold(it) }

    override suspend fun ensureJoinCode(): Household =
        delegate.ensureJoinCode().also { cacheStore.cacheHousehold(it) }

    override suspend fun revokeInvite(inviteId: Long): Household =
        delegate.revokeInvite(inviteId).also { cacheStore.cacheHousehold(it) }

    override suspend fun updateMemberRole(memberId: Long, role: MemberRole): Household =
        delegate.updateMemberRole(memberId, role).also { cacheStore.cacheHousehold(it) }

    override suspend fun removeMember(memberId: Long): Household =
        delegate.removeMember(memberId).also { cacheStore.cacheHousehold(it) }

    // Deleting the household invalidates every household-scoped cache (its members, budget,
    // categories, transactions, dashboard) — wipe the lot so a subsequent join to a different
    // household never renders the deleted one's data offline.
    override suspend fun deleteHousehold() {
        delegate.deleteHousehold()
        cacheStore.clear()
    }
}
