package com.famex.feature.profile.presentation

import com.famex.core.model.Household
import com.famex.core.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val household: Household? = null,
    val errorMessage: String? = null
)
