package com.budgeyet.feature.auth.data.remote.dto

import com.budgeyet.core.network.dto.UserResponseDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/user.py UserCreate.
@Serializable
data class SignUpRequestDto(
    val email: String,
    @SerialName("full_name") val fullName: String,
    val nickname: String,
    val pin: String
)

// Mirrors backend/app/schemas/auth.py LoginRequest.
@Serializable
data class LoginRequestDto(val email: String, val pin: String)

// Mirrors backend/app/schemas/auth.py LoginResponse (Token + user).
@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: UserResponseDto
)

// Mirrors backend/app/schemas/auth.py ForgotPinRequest.
@Serializable
data class ForgotPinRequestDto(val email: String)

// Mirrors backend/app/schemas/household.py HouseholdCreate.
@Serializable
data class HouseholdCreateRequestDto(
    val name: String,
    val currency: String,
    val language: String,
    @SerialName("cycle_start_day") val cycleStartDay: Int
)

// Mirrors backend/app/schemas/household.py JoinHouseholdRequest.
@Serializable
data class JoinHouseholdRequestDto(val token: String)

// Mirrors backend/app/schemas/budget.py BudgetCreate. month/year are omitted — there's no
// per-budget period selection in the frontend; the backend defaults an omitted month/year to
// the current cycle, which is exactly what onboarding wants anyway.
@Serializable
data class BudgetCreateRequestDto(
    val name: String,
    @SerialName("monthly_goal_amount") val monthlyGoalAmount: Double
)

// Mirrors backend/app/schemas/category.py CategoryCreate.
@Serializable
data class CategoryCreateRequestDto(
    val name: String,
    val icon: String,
    @SerialName("monthly_limit") val monthlyLimit: Double
)
