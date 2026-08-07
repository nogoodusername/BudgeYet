package com.budgeyet.core.network.mapper

import com.budgeyet.core.model.Household
import com.budgeyet.core.model.HouseholdMember
import com.budgeyet.core.model.MemberRole
import com.budgeyet.core.network.dto.HouseholdMemberResponseDto
import com.budgeyet.core.network.dto.HouseholdResponseDto
import com.budgeyet.core.network.dto.MemberRoleDto
import com.budgeyet.core.util.parseIsoDateTimeToLocalDate
import com.budgeyet.core.util.toShortMonthYearText
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
