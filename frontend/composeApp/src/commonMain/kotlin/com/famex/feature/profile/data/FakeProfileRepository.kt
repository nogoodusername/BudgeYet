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
}
