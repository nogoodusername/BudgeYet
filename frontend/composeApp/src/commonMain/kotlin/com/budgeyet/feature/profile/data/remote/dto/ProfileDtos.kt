package com.budgeyet.feature.profile.data.remote.dto

import com.budgeyet.core.network.dto.DisplayModeDto
import com.budgeyet.core.network.dto.MemberRoleDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors backend/app/schemas/user.py UserUpdate. All fields optional — RealProfileRepository
// only ever sets the one or two it's actually changing per call (name+nickname together, or
// display_mode alone), matching each ProfileRepository method's single-purpose contract.
@Serializable
data class UpdateProfileRequestDto(
    @SerialName("full_name") val fullName: String? = null,
    val nickname: String? = null,
    @SerialName("display_mode") val displayMode: DisplayModeDto? = null
)

// Mirrors backend/app/schemas/household.py HouseholdUpdate — only currency/language are ever
// sent (name/cycle_start_day have no edit UI yet).
@Serializable
data class HouseholdUpdateRequestDto(
    val currency: String? = null,
    val language: String? = null
)

// Mirrors backend/app/schemas/household.py InviteCreate.
@Serializable
data class InviteCreateRequestDto(val email: String? = null)

// Mirrors backend/app/schemas/household.py InviteResponse. Only decodes what
// core/model/Household.PendingInvite needs — expires_at/accepted_at/revoked/household_id are
// unused here (the invite-expiry copy on InviteMemberScreen reads
// Household.joinCodeExpiresAt, a household-level placeholder, not a per-invite value — see
// core/network/mapper/HouseholdMapper.kt). token is the real, backend-issued invite/join code —
// see InviteMemberUiState.joinCode, which reads it off the email-less invite.
@Serializable
data class InviteResponseDto(val id: Long, val email: String? = null, val token: String)

// Mirrors backend/app/schemas/household.py MemberRoleUpdate.
@Serializable
data class MemberRoleUpdateRequestDto(val role: MemberRoleDto)
