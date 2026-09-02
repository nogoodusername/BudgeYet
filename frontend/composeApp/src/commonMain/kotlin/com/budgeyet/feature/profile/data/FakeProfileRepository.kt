package com.budgeyet.feature.profile.data

import com.budgeyet.core.model.DisplayMode
import com.budgeyet.core.model.Household
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.model.PendingInvite
import com.budgeyet.core.model.User
import com.budgeyet.feature.profile.domain.ProfileRepository
import com.budgeyet.fixtures.DummyScenario
import com.budgeyet.fixtures.dummyCurrentUser
import com.budgeyet.fixtures.dummyHousehold
import kotlinx.coroutines.delay

class FakeProfileRepository(scenario: DummyScenario) : ProfileRepository {
    // In-memory only — mutated by the update* calls so changes persist for the rest of the
    // app session (this repository instance lives as long as AppContainer).
    private var user: User = dummyCurrentUser()
    private var household: Household = dummyHousehold(scenario)

    override suspend fun getCurrentUser(): User {
        delay(300)
        return user
    }

    override suspend fun getHousehold(): Household {
        delay(300)
        return household
    }

    override suspend fun updateProfileName(fullName: String, nickname: String): User {
        delay(300)
        user = user.copy(fullName = fullName, nickname = nickname)
        return user
    }

    override suspend fun updateDisplayMode(displayMode: DisplayMode): User {
        delay(150)
        user = user.copy(displayMode = displayMode)
        return user
    }

    override suspend fun updatePushNotifications(enabled: Boolean): User {
        delay(150)
        user = user.copy(pushNotificationsEnabled = enabled)
        return user
    }

    override suspend fun updateCurrency(currency: String): Household {
        delay(150)
        household = household.copy(currency = currency)
        return household
    }

    override suspend fun updateLanguage(language: String): Household {
        delay(150)
        household = household.copy(language = language)
        return household
    }

    override suspend fun inviteMember(email: String): Household {
        delay(400)
        check(household.members.size < Household.MAX_MEMBERS) { "Household is full" }
        val newInvite = PendingInvite(
            id = (household.pendingInvites.maxOfOrNull { it.id } ?: 0) + 1,
            email = email,
            token = "fake-invite-token-${household.pendingInvites.size + 1}"
        )
        household = household.copy(pendingInvites = household.pendingInvites + newInvite)
        return household
    }

    override suspend fun ensureJoinCode(): Household {
        delay(300)
        if (household.pendingInvites.any { it.email.isBlank() }) return household
        val newInvite = PendingInvite(
            id = (household.pendingInvites.maxOfOrNull { it.id } ?: 0) + 1,
            email = "",
            token = "FAKE-JOIN-${household.id}"
        )
        household = household.copy(pendingInvites = household.pendingInvites + newInvite)
        return household
    }

    override suspend fun revokeInvite(inviteId: Long): Household {
        delay(300)
        household = household.copy(pendingInvites = household.pendingInvites.filterNot { it.id == inviteId })
        return household
    }

    override suspend fun updateMemberRole(memberId: Long, role: MemberRole): Household {
        delay(400)
        household = household.copy(
            members = household.members.map { m ->
                when {
                    m.id == memberId -> m.copy(role = role)
                    // Owner is single-holder — promoting someone else to Owner transfers the
                    // role, demoting the previous Owner to Admin rather than leaving two Owners.
                    role == MemberRole.OWNER && m.role == MemberRole.OWNER -> m.copy(role = MemberRole.ADMIN)
                    else -> m
                }
            }
        )
        return household
    }

    override suspend fun removeMember(memberId: Long): Household {
        delay(400)
        household = household.copy(members = household.members.filterNot { it.id == memberId })
        return household
    }

    override suspend fun deleteHousehold() {
        delay(400)
        check(household.members.size == 1) { "Remove all other members before deleting the household" }
    }
}
