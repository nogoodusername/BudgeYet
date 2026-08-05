package com.famex.core.offline

import com.famex.core.cache.LocalFileStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Persistent FIFO queue of OfflineOperations, stored as a single JSON array in LocalFileStorage
// so it survives process death and cold starts. Writes rewrite the whole array (the queue is
// tiny in practice — users don't rack up hundreds of offline writes), which keeps this dead
// simple versus a per-op file or an append-only log with compaction.
class OfflineQueue(
    private val storage: LocalFileStorage,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    suspend fun enqueue(operation: OfflineOperation) {
        storage.writeString(KEY, json.encodeToString(all() + operation))
    }

    // A corrupted/partial queue file must never crash reads (and by extension an offline screen)
    // — decode defensively and treat garbage as an empty queue.
    suspend fun all(): List<OfflineOperation> =
        storage.readString(KEY)
            ?.let { raw -> runCatching { json.decodeFromString<List<OfflineOperation>>(raw) }.getOrNull() }
            ?: emptyList()

    suspend fun remove(id: String) {
        val remaining = all().filterNot { it.id == id }
        if (remaining.isEmpty()) {
            storage.remove(KEY)
        } else {
            storage.writeString(KEY, json.encodeToString(remaining))
        }
    }

    suspend fun count(): Int = all().size

    suspend fun clear() = storage.remove(KEY)

    companion object {
        const val KEY = "offline_queue_v1"
    }
}
