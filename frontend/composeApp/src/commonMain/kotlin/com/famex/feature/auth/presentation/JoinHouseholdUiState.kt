package com.famex.feature.auth.presentation

data class JoinHouseholdUiState(
    val inviteCode: String = "",
    val isJoining: Boolean = false,
    val error: String? = null
)
