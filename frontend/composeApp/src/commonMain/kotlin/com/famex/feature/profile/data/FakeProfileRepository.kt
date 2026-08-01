package com.famex.feature.profile.data

import com.famex.core.model.DisplayMode
import com.famex.core.model.Household
import com.famex.core.model.User
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCurrentUser
import com.famex.fixtures.dummyHousehold
import kotlinx.coroutines.delay

class FakeProfileRepository(scenario: DummyScenario) : ProfileRepository {
    // In-memory only — mutated by the update* calls so Save Changes persists for the rest of
    // the app session (this repository instance lives as long as AppContainer).
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

    override suspend fun updateUserProfile(
        fullName: String,
        nickname: String,
        displayMode: DisplayMode,
        pushNotificationsEnabled: Boolean
    ): User {
        delay(300)
        user = user.copy(
            fullName = fullName,
            nickname = nickname,
            displayMode = displayMode,
            pushNotificationsEnabled = pushNotificationsEnabled
        )
        return user
    }

    override suspend fun updateHouseholdSettings(currency: String, language: String): Household {
        delay(300)
        household = household.copy(currency = currency, language = language)
        return household
    }
}
