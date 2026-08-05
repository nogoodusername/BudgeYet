package com.famex.core.network

import com.famex.core.model.BackendConfig

// No hosted fam-ex deployment exists yet (see AGENTS.md — install.sh only documents self-hosting
// so far), so BackendConfig.Hosted has nothing real to resolve to yet. This placeholder uses the
// RFC 2606 reserved "example.com" domain rather than a fabricated-looking real host — update
// this constant, not call sites, once a hosted URL exists.
private const val HOSTED_BASE_URL = "https://api.famex.example.com"

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
