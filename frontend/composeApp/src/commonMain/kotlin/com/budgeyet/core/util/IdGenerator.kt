package com.budgeyet.core.util

import kotlinx.datetime.Clock
import kotlin.random.Random

// Unique id for offline queue operations and pending transactions. Timestamp + random suffix
// instead of kotlin.uuid.Uuid, which only exists in Kotlin 2.0+ — this project is pinned to
// Kotlin 1.9.23 (see AGENTS.md), and a plain string is all the queue needs.
fun randomId(prefix: String): String =
    "$prefix-${currentEpochMillis()}-${Random.nextLong().toString(36).removePrefix("-")}"

// Monotonic-ish negative id for a transaction created while offline (negative so it can never
// collide with a real server id). Timestamp-based so ids generated across process restarts
// don't collide either; the small random suffix breaks same-millisecond ties.
fun pendingTransactionTempId(): Long =
    -(currentEpochMillis() * 1000L + Random.nextInt(1000))

// kotlinx-datetime Clock is the multiplatform replacement for java.lang.System.currentTimeMillis,
// which doesn't exist on non-JVM targets (iOS/Web).
fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
