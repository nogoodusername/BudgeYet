package com.famex.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// The one shared Ktor client for the whole app (see AppContainer) — platform HTTP engine
// (OkHttp on Android, Darwin on iOS) is resolved automatically from whichever engine artifact is
// on that source set's classpath (composeApp/build.gradle.kts), so this stays platform-agnostic.
fun createHttpClient(): HttpClient = HttpClient {
    // Non-2xx responses throw ResponseException instead of returning a response the caller has
    // to status-check manually — safeApiCall (SafeApiCall.kt) is what catches it.
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }

    // Transient-failure retry only — a 4xx is a real outcome (bad input, auth, permissions) the
    // caller needs to see, not something a retry can fix.
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        retryOnException(maxRetries = 3, retryOnTimeout = true)
        exponentialDelay()
    }
}
