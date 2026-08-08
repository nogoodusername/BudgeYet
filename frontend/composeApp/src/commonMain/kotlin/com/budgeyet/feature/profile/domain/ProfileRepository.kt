package com.budgeyet.feature.profile.domain

import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.Household
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.User

interface ProfileRepository {
    suspend fun getCurrentUser(): User
    suspend fun getHousehold(): Household

    // Full name / nickname are edited as a draft and committed together via an explicit
    // Save — everything else below applies immediately on selection, one field at a time.
    suspend fun updateProfileName(fullName: String, nickname: String): User
    suspend fun updateDisplayMode(displayMode: DisplayMode): User
    suspend fun updatePushNotifications(enabled: Boolean): User
    suspend fun updateCurrency(currency: String): Household
    suspend fun updateLanguage(language: String): Household

    // Household member management (Manage Members CTA on Profile & Settings).
    // Creates a pending invite (not an immediate member add) — mirrors HouseholdService.create_invite.
    suspend fun inviteMember(email: String): Household
    // Returns the household's shareable join code as a blank-email PendingInvite, reusing an
    // existing one if the household already has one pending rather than minting a fresh token
    // (and orphaning the one already handed out) on every InviteMemberScreen load.
    suspend fun ensureJoinCode(): Household
    suspend fun revokeInvite(inviteId: Long): Household
    // Covers promote-to-Admin, promote-to-Owner (transfers ownership — see
    // FakeProfileRepository), and demote-to-Member.
    suspend fun updateMemberRole(memberId: Long, role: MemberRole): Household
    suspend fun removeMember(memberId: Long): Household
}
