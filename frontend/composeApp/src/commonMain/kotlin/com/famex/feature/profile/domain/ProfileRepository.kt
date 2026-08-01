package com.famex.feature.profile.domain

import com.famex.core.model.DisplayMode
import com.famex.core.model.Household
import com.famex.core.model.User

interface ProfileRepository {
    suspend fun getCurrentUser(): User
    suspend fun getHousehold(): Household

    suspend fun updateUserProfile(
        fullName: String,
        nickname: String,
        displayMode: DisplayMode,
        pushNotificationsEnabled: Boolean
    ): User

    suspend fun updateHouseholdSettings(currency: String, language: String): Household
}
