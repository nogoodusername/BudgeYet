package com.budgeyet.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionExpiryHttpTest {

    @Test
    fun a401FiresTheSessionExpiryNotifier() = runTest {
        val notifier = SessionExpiryNotifier()
        val fired = CompletableDeferred<Unit>()
        val job = launch { notifier.events.collect { fired.complete(Unit) } }

        val engine = MockEngine { request ->
            respond(
                content = """{"detail":"Invalid or expired access token"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        // Mirrors the real client config: expectSuccess + a response-exception handler that fires
        // the notifier on 401. With expectSuccess=true the 401 surfaces as a ClientRequestException
        // through the handler (the same path real engines take), not a delivered 200-ish response.
        val client = HttpClient(engine) {
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

        runCatching { client.get("http://localhost/thing") }

        assertEquals(Unit, fired.await())
        job.cancel()
    }

    @Test
    fun aSuccessfulResponseDoesNotFireTheNotifier() = runTest {
        val notifier = SessionExpiryNotifier()
        val engine = MockEngine { request ->
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
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

        client.get("http://localhost/thing")

        // No 401 seen — nothing buffered (no-replay + nothing emitted).
        val fired = withTimeoutOrNull(100) { notifier.events.first() }
        assertEquals(null, fired)
    }
}
