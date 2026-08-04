package com.famex.core.model

// Ordered high-to-low: OWNER is a single-holder role (transferred, not duplicated) — see
// FakeProfileRepository.updateMemberRole, which demotes the outgoing Owner to Admin whenever
// a member is promoted to Owner.
enum class MemberRole { OWNER, ADMIN, MEMBER }

data class HouseholdMember(
    val id: Long,
    val user: User,
    val role: MemberRole,
    val joinedAtText: String
)

// An invite that has been sent but not yet accepted or revoked — mirrors the backend's
// Invite row (accepted_at == null && !revoked). Doesn't count against MAX_MEMBERS; the cap
// is only enforced against actual members, same as HouseholdService.create_invite server-side.
data class PendingInvite(
    val id: Long,
    val email: String
)

// Household hard cap is 3 members (including Admin) in v1 — enforced server-side, not here.
data class Household(
    val id: Long,
    val name: String,
    val currency: String,
    val language: String,
    val cycleStartDay: Int,
    val members: List<HouseholdMember>,
    val pendingInvites: List<PendingInvite> = emptyList()
) {
    companion object {
        const val MAX_MEMBERS = 3
    }
}
