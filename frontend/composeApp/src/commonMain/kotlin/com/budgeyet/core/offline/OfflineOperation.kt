package com.budgeyet.core.offline

import com.budgeyet.core.model.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// A single write that couldn't reach the server and was parked in the offline queue
// (OfflineQueue) until connectivity returns. v1 queues transaction writes only (PRD §7:
// "Transactions can be added offline and sync automatically on reconnect") — every other
// write surface (categories, profile, members) is deliberately not queued and surfaces the
// network error inline instead.
//
// Every operation carries a stable `id` (used to remove it from the queue) and the epoch-millis
// time it was queued. Polymorphic serialization via the default "type" class discriminator.
@Serializable
sealed class OfflineOperation {
    abstract val id: String
    abstract val createdAtEpochMillis: Long

    // Creating a transaction offline. Never conflicts on sync — the server always creates a new
    // row (append-only). clientId is the same UUID stamped on the pending Transaction the UI
    // shows (Transaction.clientId); on success the returned server copy replaces it in the cache.
    @Serializable
    @SerialName("add_transaction")
    data class AddTransaction(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val clientId: String,
        val transaction: Transaction
    ) : OfflineOperation()

    // Editing an existing transaction offline. transactionId identifies server-known rows;
    // clientId identifies a row that is itself still pending (its AddTransaction sits ahead of
    // this op in the FIFO queue, and the SyncManager maps clientId -> server id as it drains).
    @Serializable
    @SerialName("update_transaction")
    data class UpdateTransaction(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val transactionId: Long?,
        val clientId: String?,
        val transaction: Transaction
    ) : OfflineOperation()

    // Deleting an existing transaction offline. Same transactionId/clientId disambiguation as
    // UpdateTransaction. Deleting a row that's already gone server-side is treated as success
    // (the end state matches), so a 404 never blocks the rest of the queue.
    @Serializable
    @SerialName("delete_transaction")
    data class DeleteTransaction(
        override val id: String,
        override val createdAtEpochMillis: Long,
        val transactionId: Long?,
        val clientId: String?
    ) : OfflineOperation()
}
