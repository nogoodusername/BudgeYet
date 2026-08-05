package com.famex.feature.transaction.data

import com.famex.core.cache.LocalCacheStore
import com.famex.core.model.Transaction
import com.famex.core.network.AppException
import com.famex.core.offline.OfflineOperation
import com.famex.core.offline.OfflineQueue
import com.famex.core.offline.SyncManager
import com.famex.core.util.currentEpochMillis
import com.famex.core.util.pendingTransactionTempId
import com.famex.core.util.randomId
import com.famex.feature.transaction.domain.TransactionRepository

// Offline-first wrapper around the real (network) TransactionRepository — the app's only write
// surface that queues for offline sync (PRD §7: "Transactions can be added offline and sync
// automatically on reconnect"). Every other repository is read-through-cache only; their writes
// deliberately surface the network error instead of queuing (see the AGENTS.md offline note).
//
// Reads: network first, falling back to the local cache on connectivity failure, then merging in
// any still-pending offline creates so they show up immediately. Writes: attempted against the
// server; on connectivity failure they're parked in the OfflineQueue (via SyncManager) and the
// UI is handed a synthetic Transaction marked pending (clientId != null) so the user sees their
// change immediately and it syncs later.
class OfflineFirstTransactionRepository(
    private val delegate: TransactionRepository,
    private val cacheStore: LocalCacheStore,
    private val queue: OfflineQueue,
    private val syncManager: SyncManager
) : TransactionRepository {

    override suspend fun getTransactions(): List<Transaction> {
        return try {
            val fresh = delegate.getTransactions()
            cacheStore.cacheTransactions(fresh)
            mergePending(fresh)
        } catch (e: AppException.NetworkException) {
            mergePending(cacheStore.getCachedTransactions() ?: throw e)
        } catch (e: AppException.TimeoutException) {
            mergePending(cacheStore.getCachedTransactions() ?: throw e)
        }
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? {
        // A pending (offline-created) transaction is findable by its negative temp id even
        // though it was never on the server.
        queue.all()
            .filterIsInstance<OfflineOperation.AddTransaction>()
            .firstOrNull { it.transaction.id == transactionId }
            ?.let { return it.transaction }

        return try {
            delegate.getTransaction(transactionId)?.also { updateCacheItem(it) }
        } catch (e: AppException.NetworkException) {
            cacheStore.getCachedTransactions()?.find { it.id == transactionId } ?: throw e
        } catch (e: AppException.TimeoutException) {
            cacheStore.getCachedTransactions()?.find { it.id == transactionId } ?: throw e
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Transaction {
        return try {
            val created = delegate.addTransaction(transaction)
            cacheStore.cacheTransactions((cacheStore.getCachedTransactions() ?: emptyList()) + created)
            created
        } catch (e: AppException.NetworkException) {
            enqueueAdd(transaction)
        } catch (e: AppException.TimeoutException) {
            enqueueAdd(transaction)
        }
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction {
        return try {
            val updated = delegate.updateTransaction(transaction)
            replaceCacheItem(updated)
            updated
        } catch (e: AppException.NetworkException) {
            enqueueUpdate(transaction)
        } catch (e: AppException.TimeoutException) {
            enqueueUpdate(transaction)
        }
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        // Deleting a transaction that itself is still pending (created offline, never synced)
        // means it never existed server-side — drop the queued Add rather than enqueueing a
        // Delete that could never resolve to a server id.
        val pendingAdd = queue.all()
            .filterIsInstance<OfflineOperation.AddTransaction>()
            .firstOrNull { it.transaction.id == transactionId }
        if (pendingAdd != null) {
            queue.remove(pendingAdd.id)
            syncManager.refreshPendingCount()
            removeCacheItem(transactionId)
            return
        }

        try {
            delegate.deleteTransaction(transactionId)
            removeCacheItem(transactionId)
        } catch (e: AppException.NetworkException) {
            enqueueDelete(transactionId)
        } catch (e: AppException.TimeoutException) {
            enqueueDelete(transactionId)
        }
    }

    override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long, toCategoryName: String) {
        // Not queued (see class doc — only transaction create/update/delete sync offline). Falls
        // through to the real repo, which throws NetworkException when offline; the controller
        // surfaces that message as-is.
        delegate.reassignCategory(fromCategoryId, toCategoryId, toCategoryName)
    }

    // ---- offline queueing helpers ----

    private suspend fun enqueueAdd(transaction: Transaction): Transaction {
        val clientId = randomId("pending")
        val pending = transaction.copy(id = pendingTransactionTempId(), clientId = clientId)
        syncManager.enqueue(
            OfflineOperation.AddTransaction(
                id = clientId,
                createdAtEpochMillis = currentEpochMillis(),
                clientId = clientId,
                transaction = pending
            )
        )
        cacheStore.cacheTransactions((cacheStore.getCachedTransactions() ?: emptyList()) + pending)
        return pending
    }

    private suspend fun enqueueUpdate(transaction: Transaction): Transaction {
        syncManager.enqueue(
            OfflineOperation.UpdateTransaction(
                id = randomId("update"),
                createdAtEpochMillis = currentEpochMillis(),
                transactionId = if (transaction.isPending) null else transaction.id,
                clientId = transaction.clientId,
                transaction = transaction
            )
        )
        // Optimistic local update so the history list reflects the change immediately.
        replaceCacheItem(transaction)
        return transaction
    }

    private suspend fun enqueueDelete(transactionId: Long) {
        syncManager.enqueue(
            OfflineOperation.DeleteTransaction(
                id = randomId("delete"),
                createdAtEpochMillis = currentEpochMillis(),
                transactionId = transactionId,
                clientId = null
            )
        )
        removeCacheItem(transactionId)
    }

    // Pending creates (clientId != null) replace any cached copy of themselves; everything else
    // merges after the server-fetched rows. Only the queue's Add operations are merged — queued
    // updates/deletes are already reflected in the cache (optimistic write-back above), so
    // merging them again would double-apply.
    private suspend fun mergePending(serverOrCached: List<Transaction>): List<Transaction> {
        val pendingAdds = queue.all()
            .filterIsInstance<OfflineOperation.AddTransaction>()
            .map { it.transaction }
        if (pendingAdds.isEmpty()) return serverOrCached
        val pendingClientIds = pendingAdds.mapNotNull { it.clientId }.toSet()
        return pendingAdds + serverOrCached.filterNot { it.clientId in pendingClientIds }
    }

    private suspend fun updateCacheItem(tx: Transaction) {
        val current = cacheStore.getCachedTransactions() ?: return
        cacheStore.cacheTransactions(current.map { if (it.id == tx.id) tx else it })
    }

    private suspend fun replaceCacheItem(tx: Transaction) {
        val current = cacheStore.getCachedTransactions() ?: emptyList()
        val index = current.indexOfFirst { it.id == tx.id || (tx.clientId != null && it.clientId == tx.clientId) }
        val updated = if (index >= 0) current.mapIndexed { i, item -> if (i == index) tx else item } else current + tx
        cacheStore.cacheTransactions(updated)
    }

    private suspend fun removeCacheItem(transactionId: Long) {
        val current = cacheStore.getCachedTransactions() ?: return
        cacheStore.cacheTransactions(current.filterNot { it.id == transactionId })
    }
}
