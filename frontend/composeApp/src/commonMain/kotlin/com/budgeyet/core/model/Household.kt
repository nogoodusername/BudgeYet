package com.budgeyet.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable

// Ordered high-to-low: OWNER is a single-holder role (transferred, not duplicated) — see
// FakeProfileRepository.updateMemberRole, which demotes the outgoing Owner to Admin whenever
// a member is promoted to Owner.
enum class MemberRole {
    OWNER, ADMIN, MEMBER;

    // Admin *or* Owner can manage household settings and members (PRD §5) — Owner is a
    // superset of Admin, so this is the check the profile/members UI gates its admin-only
    // actions behind. Member is the only role it excludes.
    val isAdminOrOwner: Boolean
        get() = this == ADMIN || this == OWNER
}

@Serializable
data class HouseholdMember(
    val id: Long,
    val user: User,
    val role: MemberRole,
    val joinedAtText: String
)

// An invite that has been sent but not yet accepted or revoked — mirrors the backend's
// Invite row (accepted_at == null && !revoked). Doesn't count against MAX_MEMBERS; the cap
// is only enforced against actual members, same as HouseholdService.create_invite server-side.
@Serializable
data class PendingInvite(
    val id: Long,
    val email: String
)

// Household hard cap is 3 members (including Admin) in v1 — enforced server-side, not here.
// @Serializable (and its nested types above) so AuthSession — which carries a Household — can
// round-trip through AuthRepository.persistSession/getPersistedSession.
@Serializable
data class Household(
    val id: Long,
    val name: String,
    val currency: String,
    val language: String,
    val cycleStartDay: Int,
    val members: List<HouseholdMember>,
    val pendingInvites: List<PendingInvite> = emptyList(),
    // Mirrors the backend's Invite.expires_at for the household's join code (see
    // INVITE_EXPIRY_DAYS server-side) — drives the "expire automatically on {date}" copy on
    // InviteMemberScreen. kotlinx-datetime's LocalDate isn't @Serializable on its own — this
    // is the built-in ISO-8601 (yyyy-MM-dd) serializer kotlinx-datetime ships for exactly this.
    @Serializable(with = LocalDateIso8601Serializer::class)
    val joinCodeExpiresAt: LocalDate
) {
    // The signed-in member's role in this household, looked up by user id. null when the
    // userId doesn't match any member (shouldn't happen inside a household, but callers must
    // treat null as "no admin privileges" rather than assume a role).
    fun currentMemberRole(userId: Long?): MemberRole? =
        userId?.let { id -> members.find { it.user.id == id }?.role }

    companion object {
        const val MAX_MEMBERS = 3
        const val JOIN_CODE_EXPIRY_DAYS = 7
    }
}
