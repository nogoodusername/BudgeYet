package com.budgeyet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
// The one shared Ktor client for the whole app (see AppContainer) — platform HTTP engine
// (OkHttp on Android, Darwin on iOS) is resolved automatically from whichever engine artifact is
// on that source set's classpath (composeApp/build.gradle.kts), so this stays platform-agnostic.
fun createHttpClient(sessionExpiryNotifier: SessionExpiryNotifier? = null): HttpClient = HttpClient {
    // Non-2xx responses throw ResponseException instead of returning a response the caller has
    // to status-check manually — safeApiCall (SafeApiCall.kt) is what catches it.
    expectSuccess = true

    // A 401 anywhere means the access token is dead (no refresh token to renew it with — see
    // AuthTokenStorage). Fire the session-expiry signal so App.kt can drop the user to
    // onboarding instead of stranding them on the failing screen's error state.
    //
    // This MUST be a response-exception handler, not a receive-pipeline observer:
    // with expectSuccess = true Ktor validates the status inside the HttpSend plugin
    // (HttpCallValidator.validateResponse throws for 401 before the response reaches the receive
    // pipeline), so a ResponseObserver hooked on HttpReceivePipeline.After never sees a 401 on a
    // real engine — the response is never delivered downstream. handleResponseExceptionWithRequest
    // is invoked on exactly that thrown exception, so it sees every 401 the app gets.
    if (sessionExpiryNotifier != null) {
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                if (cause is ClientRequestException && cause.response.status == HttpStatusCode.Unauthorized) {
                    sessionExpiryNotifier.notifyExpired()
                }
            }
        }
    }

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
