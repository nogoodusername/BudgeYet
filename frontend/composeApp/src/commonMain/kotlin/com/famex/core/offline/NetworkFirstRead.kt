package com.famex.core.offline

import com.famex.core.network.AppException

// Network-first read with cache fallback — the read policy every OfflineFirst*Repository uses
// (see AGENTS.md "offline support" note): try the server, and only when connectivity itself
// failed (unreachable/timeout) fall back to the cached copy. Every other error — auth, permission,
// validation — propagates untouched, because a stale cache must not mask a real rejection. When
// the cache is also empty the original network exception is rethrown so the controller's error
// state still renders.
suspend fun <T> networkFirstRead(
    networkCall: suspend () -> T,
    cached: suspend () -> T?,
    onSuccess: suspend (T) -> Unit = {}
): T {
    try {
        val value = networkCall()
        onSuccess(value)
        return value
    } catch (e: AppException.NetworkException) {
        return cached() ?: throw e
    } catch (e: AppException.TimeoutException) {
        return cached() ?: throw e
    }
}
