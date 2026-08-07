package com.budgeyet.core.offline

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.network.AppException
import com.budgeyet.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// User-facing outcomes of a sync pass. Rejected fires per discarded operation (conflict /
// permanent rejection) so the UI can tell the user an offline change wasn't applied.
sealed interface SyncEvent {
    data class Rejected(val message: String) : SyncEvent
}

private enum class ReplayOutcome { SUCCESS, DISCARDED, RETRY_LATER }

// Owns the offline write queue: accepts new operations from the OfflineFirstTransactionRepository
// and drains them against the real (network) TransactionRepository once connectivity returns
// (App.kt triggers processQueue on every offline→online transition). Conflict resolution is
// ConflictResolver's job — v1 is "server wins, append only safe".
//
// Exposes pendingCount (StateFlow) for the UI's "N changes waiting to sync" badge and events
// (SharedFlow) for rejected-change toasts. The delegate here MUST be the real repository — never
// the OfflineFirst wrapper — or replay would re-queue instead of replaying.
class SyncManager(
    private val transactionRepository: TransactionRepository,
    private val cacheStore: LocalCacheStore,
    private val queue: OfflineQueue
) {
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>(
        // replay = 1 so a rejection emitted while no collector is attached (e.g. between screen
        // swaps) isn't silently dropped — the most recent outcome is handed to the next collector.
        replay = 1,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    // clientId (of a pending offline-created transaction) -> server id, learned as each
    // AddTransaction in the queue is replayed. Subsequent Update/Delete ops that reference the
    // same clientId resolve through this. Not persisted: it only needs to live for the duration
    // of one drain, and the queue is FIFO so the Add always precedes the ops that reference it.
    private val clientIdToServerId = mutableMapOf<String, Long>()

    private val mutex = Mutex()

    suspend fun enqueue(operation: OfflineOperation) {
        queue.enqueue(operation)
        refreshPendingCount()
    }

    // Re-reads the queue's size into pendingCount. Called after any direct queue mutation that
    // bypasses enqueue — e.g. OfflineFirstTransactionRepository deleting a still-pending create
    // outright instead of queueing a Delete for it.
    suspend fun refreshPendingCount() {
        _pendingCount.value = queue.count()
    }

    // Drains the queue FIFO. Stops at the first RETRY_LATER (network hiccup mid-drain, 5xx, 429)
    // and leaves the rest queued for the next reconnect — replaying mid-drain would just fail the
    // same way and could reorder operations. A single drain must not run concurrently.
    suspend fun processQueue() {
        mutex.withLock {
            clientIdToServerId.clear()
            while (true) {
                val next = queue.all().firstOrNull() ?: break
                when (replay(next)) {
                    ReplayOutcome.SUCCESS, ReplayOutcome.DISCARDED -> queue.remove(next.id)
                    ReplayOutcome.RETRY_LATER -> break
                }
            }
            refreshPendingCount()
        }
    }

    private suspend fun replay(op: OfflineOperation): ReplayOutcome = when (op) {
        is OfflineOperation.AddTransaction -> replayAdd(op)
        is OfflineOperation.UpdateTransaction -> replayUpdate(op)
        is OfflineOperation.DeleteTransaction -> replayDelete(op)
    }

    private suspend fun replayAdd(op: OfflineOperation.AddTransaction): ReplayOutcome = try {
        // The server's copy is authoritative — clear the pending clientId/temp-id markers so the
        // confirmed row is indistinguishable from a server-fetched one (isPending becomes false).
        val serverTx = transactionRepository.addTransaction(op.transaction).copy(clientId = null)
        clientIdToServerId[op.clientId] = serverTx.id
        cacheStore.cacheTransactions(replaceByClientId(op.clientId, serverTx))
        ReplayOutcome.SUCCESS
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (ConflictResolver.resolve(e) == SyncResolution.RETRY_LATER) {
            ReplayOutcome.RETRY_LATER
        } else {
            cacheStore.cacheTransactions(replaceByClientId(op.clientId, null))
            emitRejected("${op.transaction.merchant} wasn't saved: ${e.message}")
            ReplayOutcome.DISCARDED
        }
    }

    private suspend fun replayUpdate(op: OfflineOperation.UpdateTransaction): ReplayOutcome {
        val serverId = resolveServerId(op.transactionId, op.clientId)
            ?: return emitRejected("That change couldn't be applied: ${op.transaction.merchant}")
        return try {
            val serverTx = transactionRepository.updateTransaction(op.transaction.copy(id = serverId)).copy(clientId = null)
            cacheStore.cacheTransactions(replaceByServerId(serverId, serverTx))
            ReplayOutcome.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            when (ConflictResolver.resolve(e)) {
                SyncResolution.RETRY_LATER -> ReplayOutcome.RETRY_LATER
                SyncResolution.DISCARD -> emitRejected("Your offline change to ${op.transaction.merchant} was discarded: ${e.message}")
            }
        }
    }

    private suspend fun replayDelete(op: OfflineOperation.DeleteTransaction): ReplayOutcome {
        val serverId = resolveServerId(op.transactionId, op.clientId)
            ?: return emitRejected("That change couldn't be applied")
        return try {
            transactionRepository.deleteTransaction(serverId)
            cacheStore.cacheTransactions(removeByServerId(serverId))
            ReplayOutcome.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (e: AppException.NotFoundException) {
            // Already gone server-side — the delete's end state is achieved, not an error.
            cacheStore.cacheTransactions(removeByServerId(serverId))
            ReplayOutcome.SUCCESS
        } catch (e: Exception) {
            when (ConflictResolver.resolve(e)) {
                SyncResolution.RETRY_LATER -> ReplayOutcome.RETRY_LATER
                SyncResolution.DISCARD -> emitRejected("That change couldn't be applied: ${e.message}")
            }
        }
    }

    // The queued op's transactionId wins for server-known rows; clientId (a still-pending create)
    // resolves through the map the AddTransaction replay populated. null when neither applies —
    // e.g. a follow-up op referencing a pending create whose Add was rejected — and the op is
    // discarded.
    private fun resolveServerId(transactionId: Long?, clientId: String?): Long? =
        transactionId ?: clientId?.let { clientIdToServerId[it] }

    // Removes any pending copy (matched by clientId) and inserts the server-confirmed row. When
    // syncTx is null (the Add was rejected) it just drops the pending copy — the row never reached
    // the server so the cache shouldn't pretend it did.
    private suspend fun replaceByClientId(clientId: String, syncTx: Transaction?): List<Transaction> {
        val current = cacheStore.getCachedTransactions() ?: emptyList()
        val withoutPending = current.filterNot { it.clientId == clientId }
        return if (syncTx == null) withoutPending else withoutPending + syncTx
    }

    private suspend fun replaceByServerId(serverId: Long, syncTx: Transaction): List<Transaction> {
        val current = cacheStore.getCachedTransactions() ?: emptyList()
        return current.map { if (it.id == serverId) syncTx else it }
    }

    private suspend fun removeByServerId(serverId: Long): List<Transaction> {
        val current = cacheStore.getCachedTransactions() ?: emptyList()
        return current.filterNot { it.id == serverId }
    }

    private suspend fun emitRejected(message: String): ReplayOutcome {
        _events.emit(SyncEvent.Rejected(message))
        return ReplayOutcome.DISCARDED
    }
}
