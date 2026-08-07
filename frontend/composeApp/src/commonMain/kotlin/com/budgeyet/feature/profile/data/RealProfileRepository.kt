package com.budgeyet.feature.profile.data

import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.Household
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.User
import com.budgeyet.core.network.AppException
import com.budgeyet.core.network.HouseholdRequestContextProvider
import com.budgeyet.core.network.mapper.toDomain
import com.budgeyet.core.network.mapper.toDto
import com.budgeyet.feature.profile.data.mapper.toDomain
import com.budgeyet.feature.profile.data.remote.ProfileApiService
import com.budgeyet.feature.profile.data.remote.dto.HouseholdUpdateRequestDto
import com.budgeyet.feature.profile.data.remote.dto.UpdateProfileRequestDto
import com.budgeyet.feature.profile.domain.ProfileRepository

// Real, network-backed ProfileRepository — same shape as the other Real*Repository
// implementations, resolving household id via HouseholdRequestContextProvider.
class RealProfileRepository(
    private val api: ProfileApiService,
    private val contextProvider: HouseholdRequestContextProvider
) : ProfileRepository {

    override suspend fun getCurrentUser(): User {
        val (config, token, _) = contextProvider.get()
        return api.getMe(config, token).toDomain()
    }

    override suspend fun getHousehold(): Household = fetchHousehold()

    override suspend fun updateProfileName(fullName: String, nickname: String): User {
        val (config, token, _) = contextProvider.get()
        return api.updateMe(config, token, UpdateProfileRequestDto(fullName = fullName, nickname = nickname)).toDomain()
    }

    override suspend fun updateDisplayMode(displayMode: DisplayMode): User {
        val (config, token, _) = contextProvider.get()
        return api.updateMe(config, token, UpdateProfileRequestDto(displayMode = displayMode.toDto())).toDomain()
    }

    // No backend field for this yet (UserResponse has no push-notification flag — see
    // core/network/mapper/UserMapper.kt's toDomain, which always defaults it to true). Applied
    // locally on top of a fresh /users/me fetch rather than persisted anywhere, so it resets to
    // the default the next time the user/session is reloaded — a known, documented gap, not a bug.
    override suspend fun updatePushNotifications(enabled: Boolean): User {
        val (config, token, _) = contextProvider.get()
        return api.getMe(config, token).toDomain().copy(pushNotificationsEnabled = enabled)
    }

    override suspend fun updateCurrency(currency: String): Household {
        val (config, token, householdId) = contextProvider.get()
        api.updateHousehold(config, token, householdId, HouseholdUpdateRequestDto(currency = currency))
        return fetchHousehold()
    }

    override suspend fun updateLanguage(language: String): Household {
        val (config, token, householdId) = contextProvider.get()
        api.updateHousehold(config, token, householdId, HouseholdUpdateRequestDto(language = language))
        return fetchHousehold()
    }

    override suspend fun inviteMember(email: String): Household {
        val (config, token, householdId) = contextProvider.get()
        api.createInvite(config, token, householdId, email)
        return fetchHousehold()
    }

    override suspend fun revokeInvite(inviteId: Long): Household {
        val (config, token, householdId) = contextProvider.get()
        api.revokeInvite(config, token, householdId, inviteId)
        return fetchHousehold()
    }

    override suspend fun updateMemberRole(memberId: Long, role: MemberRole): Household {
        val (config, token, householdId) = contextProvider.get()
        api.updateMemberRole(config, token, householdId, memberId, role.toDto())
        return fetchHousehold()
    }

    override suspend fun removeMember(memberId: Long): Household {
        val (config, token, householdId) = contextProvider.get()
        api.removeMember(config, token, householdId, memberId)
        return fetchHousehold()
    }

    // Pending invites live on a separate admin-only endpoint (GET .../invites) — HouseholdResponse
    // never includes them (see core/network/mapper/HouseholdMapper.kt). This screen doesn't
    // currently gate member-management actions by role at all (a pre-existing frontend gap, see
    // AGENTS.md), so a plain Member landing here would otherwise hard-fail on a 403 just loading
    // the screen; treat "can't see invites" as "no invites to show" instead of an error.
    private suspend fun fetchHousehold(): Household {
        val (config, token, householdId) = contextProvider.get()
        val household = api.getHousehold(config, token, householdId).toDomain()
        val pendingInvites = try {
            api.listInvites(config, token, householdId).map { it.toDomain() }
        } catch (e: AppException.PermissionDeniedException) {
            emptyList()
        }
        return household.copy(pendingInvites = pendingInvites)
    }
}
