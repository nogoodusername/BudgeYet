package com.budgeyet.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// App-wide broadcast for "the server rejected our access token" (HTTP 401). Wired into the
// shared HTTP client (HttpClientFactory) so any authenticated call that comes back 401 —
// Dashboard load, History, Profile, a category edit, a queued-offline-op replay — fires it,
// no matter which screen is visible.
//
// Why auto-sign-out: the backend issues a plain access token with no refresh token (see
// AuthTokenStorage), so a server-rejected token can never be renewed — re-auth is the only
// path. Without this, the user lands on the failing screen's error state with just a Retry
// button (the old dead end: "Invalid or expired access token" with nowhere to go). App.kt
// collects this and drops the user to onboarding, clearing the persisted session/token.
//
// No replay: an expiry is a one-shot event per session — replaying a stale one into a fresh
// composition would sign out a user who logged back in fine.
class SessionExpiryNotifier {
    private val _events = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notify() {
        _events.tryEmit(Unit)
    }
}
