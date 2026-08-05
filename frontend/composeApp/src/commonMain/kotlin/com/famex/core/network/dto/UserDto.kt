package com.famex.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/user.py UserResponse. Shared across features (auth, transaction's
// paid_by_user/created_by_user, ...) rather than duplicated per feature — everything that talks
// to the backend eventually needs to decode a nested user.
@Serializable
data class UserResponseDto(
    val id: Long,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val nickname: String,
    @SerialName("display_mode") val displayMode: DisplayModeDto
)

@Serializable
enum class DisplayModeDto {
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
    @SerialName("system") SYSTEM
}
