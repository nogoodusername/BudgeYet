package com.famex.feature.profile.domain

import com.famex.core.model.Household
import com.famex.core.model.User

interface ProfileRepository {
    suspend fun getCurrentUser(): User
    suspend fun getHousehold(): Household
}
