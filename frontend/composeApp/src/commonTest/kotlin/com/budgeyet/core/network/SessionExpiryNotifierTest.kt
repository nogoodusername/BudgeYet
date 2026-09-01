package com.budgeyet.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SessionExpiryNotifierTest {

    // App.kt collects events while signed in; if the collector were gone (e.g. the signed-in
    // composition was disposed mid-render), a 401 fired into the notifier must NOT be replayed
    // into the next composition that subscribes — that would sign out a user who logged back in.
    // This pins the no-replay (replay = 0) contract.
    @Test
    fun eventsAreNotReplayedToLateCollectors() = runTest {
        val notifier = SessionExpiryNotifier()
        notifier.notify()

        // A late collector must not see the earlier notify(): with no replay buffer, first()
        // suspends forever, so withTimeoutOrNull returns null instead.
        val seen = withTimeoutOrNull(100) { notifier.events.first() }
        assertNull(seen)
    }

    @Test
    fun eventsAreDeliveredToAnActiveCollector() = runTest {
        val notifier = SessionExpiryNotifier()
        val received = CompletableDeferred<Unit>()

        // Collect on the test coroutine's child so the collector is guaranteed to be subscribed
        // before notify() runs (backgroundScope defers dispatch, so the event could be emitted
        // before the collector attaches and get dropped — the exact failure we saw).
        val job = launch { notifier.events.collect { received.complete(Unit) } }
        runCurrent() // Actually start the collector (runTest uses a virtual-time dispatcher).
        notifier.notify()

        assertEquals(Unit, received.await())
        job.cancel()
    }
}
