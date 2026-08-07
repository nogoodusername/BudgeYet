package com.budgeyet.core.network

import com.budgeyet.core.model.BackendConfig

// Hosted deployment at budge-yet-api.imhx.top — update this constant, not call sites, if the
// hosted URL changes.
private const val HOSTED_BASE_URL = "https://budge-yet-api.imhx.top"

private const val API_PREFIX = "/api/v1"

// Centralizes the base-URL trimming + /api/v1 prefixing every repository needs to turn a
// BackendConfig into a request URL — mirrors what FakeAuthRepository.checkServerReachable did
// ad hoc for /api/v1/ping before this existed.
fun BackendConfig.apiUrl(path: String): String {
    val base = when (this) {
        BackendConfig.Hosted -> HOSTED_BASE_URL
        is BackendConfig.Custom -> url
    }.trim().trimEnd('/')
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return "$base$API_PREFIX$normalizedPath"
}
