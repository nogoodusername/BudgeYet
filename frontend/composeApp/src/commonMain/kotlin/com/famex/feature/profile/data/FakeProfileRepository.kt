package com.famex.feature.profile.data

import com.famex.core.model.DisplayMode
import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole
import com.famex.core.model.User
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCurrentUser
import com.famex.fixtures.dummyHousehold
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
        val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        val nextUserId = (household.members.maxOfOrNull { it.user.id } ?: 0) + 1
        val invitedUser = User(id = nextUserId, email = email, fullName = displayName, nickname = displayName)
        val newMember = HouseholdMember(
            id = (household.members.maxOfOrNull { it.id } ?: 0) + 1,
            user = invitedUser,
            role = MemberRole.MEMBER,
            joinedAtText = "Just now"
        )
        household = household.copy(members = household.members + newMember)
        return household
    }

    override suspend fun promoteToAdmin(memberId: Long): Household {
        delay(400)
        household = household.copy(
            members = household.members.map { if (it.id == memberId) it.copy(role = MemberRole.ADMIN) else it }
        )
        return household
    }

    override suspend fun removeMember(memberId: Long): Household {
        delay(400)
        household = household.copy(members = household.members.filterNot { it.id == memberId })
        return household
    }
}
