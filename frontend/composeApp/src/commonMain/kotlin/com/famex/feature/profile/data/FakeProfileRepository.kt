package com.famex.feature.profile.data

import com.famex.core.model.Household
import com.famex.core.model.User
import com.famex.feature.profile.domain.ProfileRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyCurrentUser
import com.famex.fixtures.dummyHousehold
import kotlinx.coroutines.delay

class FakeProfileRepository(private val scenario: DummyScenario) : ProfileRepository {
    override suspend fun getCurrentUser(): User {
        delay(300)
        return dummyCurrentUser()
    }

    override suspend fun getHousehold(): Household {
        delay(300)
        return dummyHousehold(scenario)
    }
}
