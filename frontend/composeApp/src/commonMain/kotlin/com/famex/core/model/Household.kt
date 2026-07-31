package com.famex.core.model

enum class MemberRole { ADMIN, MEMBER }

data class HouseholdMember(
    val id: Long,
    val user: User,
    val role: MemberRole,
    val joinedAtText: String
)

// Household hard cap is 3 members (including Admin) in v1 — enforced server-side, not here.
data class Household(
    val id: Long,
    val name: String,
    val currency: String,
    val language: String,
    val cycleStartDay: Int,
    val members: List<HouseholdMember>
)
