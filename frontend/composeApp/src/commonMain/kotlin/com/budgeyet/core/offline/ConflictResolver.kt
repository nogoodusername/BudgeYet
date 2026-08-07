package com.budgeyet.core.offline

import com.budgeyet.core.network.AppException

// What SyncManager should do with a queued operation that just failed to replay.
enum class SyncResolution {
    // Keep the operation queued and stop draining — connectivity/transient issues that a later
    // sync attempt (next reconnect) may clear. Never silently drop a user's offline change here.
    RETRY_LATER,

    // Remove the operation and surface a message to the user — a definitive server rejection
    // (conflict, validation, permission) where the server's version wins. Replaying it again
    // would just fail the same way.
    DISCARD
}

// Conflict-safe sync strategy (PRD §7). v1 is deliberately conservative: "server wins, append
// only safe".
//
// - Creating a transaction never conflicts — the server always appends a new row, so an
//   AddTransaction can only fail for transient (RETRY_LATER) or hard-reject (DISCARD) reasons.
// - Editing/deleting resolves in the server's favor: the server's copy of the data is truth, so
//   a rejected local change is discarded rather than merged.
// - Any error the server could plausibly recover from on its own (5xx, 429) leaves the change
//   queued for the next sync attempt instead of throwing it away.
object ConflictResolver {
    fun resolve(error: Throwable): SyncResolution = when (error) {
        is AppException.NetworkException,
        is AppException.TimeoutException,
        is AppException.ServerException,
        is AppException.RateLimitException,
        is AppException.AuthenticationException -> SyncResolution.RETRY_LATER
        // NotFound on a delete means the server already deleted it — desired end state, not an error.
        is AppException.NotFoundException,
        is AppException.ConflictException,
        is AppException.ValidationException,
        is AppException.PermissionDeniedException,
        is AppException.UnknownException -> SyncResolution.DISCARD
        else -> SyncResolution.DISCARD
    }
}
