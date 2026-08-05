package com.famex.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/household.py HouseholdResponse. Shared across features (Auth's
// login/createHousehold/joinHousehold, Dashboard's household header) rather than duplicated —
// both need the exact same shape.
@Serializable
data class HouseholdResponseDto(
    val id: Long,
    val name: String,
    val currency: String,
    val language: String,
    @SerialName("cycle_start_day") val cycleStartDay: Int,
    val members: List<HouseholdMemberResponseDto> = emptyList(),
    @SerialName("created_at") val createdAt: String
)

// Mirrors backend/app/schemas/household.py HouseholdMemberResponse.
@Serializable
data class HouseholdMemberResponseDto(
    val id: Long,
    @SerialName("household_id") val householdId: Long,
    val user: UserResponseDto,
    val role: MemberRoleDto,
    @SerialName("joined_at") val joinedAt: String
)

@Serializable
enum class MemberRoleDto {
    @SerialName("owner") OWNER,
    @SerialName("admin") ADMIN,
    @SerialName("member") MEMBER
}
