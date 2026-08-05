package com.famex.core.network.mapper

import com.famex.core.model.Household
import com.famex.core.model.HouseholdMember
import com.famex.core.model.MemberRole
import com.famex.core.network.dto.HouseholdMemberResponseDto
import com.famex.core.network.dto.HouseholdResponseDto
import com.famex.core.network.dto.MemberRoleDto
import com.famex.core.util.parseIsoDateTimeToLocalDate
import com.famex.core.util.toShortMonthYearText
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

fun MemberRoleDto.toDomain(): MemberRole = when (this) {
    MemberRoleDto.OWNER -> MemberRole.OWNER
    MemberRoleDto.ADMIN -> MemberRole.ADMIN
    MemberRoleDto.MEMBER -> MemberRole.MEMBER
}

fun MemberRole.toDto(): MemberRoleDto = when (this) {
    MemberRole.OWNER -> MemberRoleDto.OWNER
    MemberRole.ADMIN -> MemberRoleDto.ADMIN
    MemberRole.MEMBER -> MemberRoleDto.MEMBER
}

fun HouseholdMemberResponseDto.toDomain(): HouseholdMember = HouseholdMember(
    id = id,
    user = user.toDomain(),
    role = role.toDomain(),
    joinedAtText = parseIsoDateTimeToLocalDate(joinedAt).toShortMonthYearText()
)

// The backend has no household-level "join code" concept — only per-invite tokens, each with
// their own expires_at (backend/app/schemas/household.py InviteResponse), and HouseholdResponse
// doesn't return them at all (see backend/app/api/v1/endpoints/households.py — invites are a
// separate admin-only list). Household.joinCodeExpiresAt is a placeholder (createdAt + 7 days,
// matching INVITE_EXPIRY_DAYS) until the shareable join link/QR feature (still an open gap —
// see AGENTS.md Phase 3) actually threads a real invite through this model.
fun HouseholdResponseDto.toDomain(): Household {
    val createdAt = parseIsoDateTimeToLocalDate(createdAt)
    return Household(
        id = id,
        name = name,
        currency = currency,
        language = language,
        cycleStartDay = cycleStartDay,
        members = members.map { it.toDomain() },
        pendingInvites = emptyList(),
        joinCodeExpiresAt = createdAt.plus(Household.JOIN_CODE_EXPIRY_DAYS, DateTimeUnit.DAY)
    )
}
