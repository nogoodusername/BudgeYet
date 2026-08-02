package com.famex.feature.profile.domain

import com.famex.core.model.DisplayMode
import com.famex.core.model.Household
import com.famex.core.model.MemberRole
import com.famex.core.model.User

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
    suspend fun inviteMember(email: String): Household
    // Covers promote-to-Admin, promote-to-Owner (transfers ownership — see
    // FakeProfileRepository), and demote-to-Member.
    suspend fun updateMemberRole(memberId: Long, role: MemberRole): Household
    suspend fun removeMember(memberId: Long): Household
}
