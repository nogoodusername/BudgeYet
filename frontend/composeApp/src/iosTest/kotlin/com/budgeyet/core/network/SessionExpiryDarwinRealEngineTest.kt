package com.budgeyet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

// Decisive test: runs on the iOS SIMULATOR against the REAL local backend using the REAL Darwin
// engine (not MockEngine). Verifies that a genuine 401 (bad access token) fires the
// SessionExpiryNotifier — the exact scenario the user hit on a physical device.
// Requires the local FastAPI backend running on localhost:8000 (uvicorn app.main:app).
class SessionExpiryDarwinRealEngineTest {

    @Test
    fun real401FiresTheNotifierOnTheDarwinEngine() = runTest {
        val notifier = SessionExpiryNotifier()
        val fired = CompletableDeferred<Unit>()
        val job = launch { notifier.events.collect { fired.complete(Unit) } }

        val client = HttpClient(Darwin) {
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    if (cause is io.ktor.client.plugins.ClientRequestException &&
                        cause.response.status == HttpStatusCode.Unauthorized
                    ) {
                        notifier.notify()
                    }
                }
            }
        }

        // Hit an authenticated endpoint with a bogus token → real 401 from the real backend.
        runCatching {
            client.get("http://localhost:8000/api/v1/users/me") {
                headers.append("Authorization", "Bearer definitely-not-a-real-token")
            }
        }

        // If the mechanism is correct, the notifier fires. If the exception never reaches the
        // handler (e.g. the 401 throws elsewhere), this await() times out and the test fails.
        val result = withTimeout(10_000) { fired.await() }
        assertEquals(Unit, result)
        job.cancel()
    }
}
