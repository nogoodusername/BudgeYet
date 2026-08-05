package com.famex.core.offline

import com.famex.core.cache.LocalCacheStore
import com.famex.core.network.AppException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class SyncManagerTest {

    private fun fixture(delegate: FakeTransactionRepository): Pair<SyncManager, LocalCacheStore> {
        val cacheStore = LocalCacheStore(InMemoryLocalFileStorage())
        val manager = SyncManager(
            transactionRepository = delegate,
            cacheStore = cacheStore,
            queue = OfflineQueue(InMemoryLocalFileStorage())
        )
        return manager to cacheStore
    }

    @Test
    fun drainReplaysPendingAddAgainstTheServer() = runTest {
        val delegate = FakeTransactionRepository()
        val (manager, cacheStore) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.AddTransaction(
                id = "pending-1",
                createdAtEpochMillis = 1L,
                clientId = "c1",
                transaction = testTransaction(id = -1, clientId = "c1", merchant = "Groceries")
            )
        )
        assertEquals(1, manager.pendingCount.value)

        manager.processQueue()

        assertEquals("Groceries", delegate.added.single().merchant)
        assertEquals(0, manager.pendingCount.value)
        // The server copy replaced the pending copy in the cache (positive id, no clientId).
        val cached = cacheStore.getCachedTransactions().orEmpty()
        assertEquals(1, cached.size)
        assertEquals(1000L, cached.single().id)
        assertNull(cached.single().clientId)
    }

    @Test
    fun networkFailureAbortsDrainAndKeepsTheOperationQueued() = runTest {
        val delegate = FakeTransactionRepository().apply { addError = AppException.NetworkException() }
        val (manager, _) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", testTransaction(-1, "c1"))
        )

        manager.processQueue()

        assertTrue(delegate.added.isEmpty())
        assertEquals(1, manager.pendingCount.value)
    }

    @Test
    fun conflictOnAddDiscardsTheOperationAndEmitsRejected() = runTest {
        val delegate = FakeTransactionRepository().apply { addError = AppException.ConflictException("nope") }
        val (manager, _) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", testTransaction(-1, "c1", "Rent"))
        )

        manager.processQueue()

        assertEquals(0, manager.pendingCount.value)
        val event = manager.events.first() as SyncEvent.Rejected
        assertTrue(event.message.contains("Rent"), "rejection should name the merchant, got: ${event.message}")
    }

    @Test
    fun updateReferencingPendingCreateResolvesThroughClientId() = runTest {
        val delegate = FakeTransactionRepository()
        val (manager, _) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", testTransaction(-1, "c1", "Groceries"))
        )
        manager.enqueue(
            OfflineOperation.UpdateTransaction(
                id = "update-1",
                createdAtEpochMillis = 2L,
                transactionId = null,
                clientId = "c1",
                transaction = testTransaction(id = -1, clientId = "c1", merchant = "Groceries v2")
            )
        )

        manager.processQueue()

        // Add created server id 1000; the update must have been applied against that id.
        assertEquals(1000L, delegate.updated.single().id)
        assertEquals(0, manager.pendingCount.value)
    }

    @Test
    fun updateReferencingUnknownPendingCreateIsDiscardedNotRetried() = runTest {
        val delegate = FakeTransactionRepository()
        val (manager, _) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.UpdateTransaction(
                id = "update-1",
                createdAtEpochMillis = 1L,
                transactionId = null,
                clientId = "ghost", // no preceding AddTransaction in the queue
                transaction = testTransaction(id = -1, clientId = "ghost")
            )
        )

        manager.processQueue()

        assertTrue(delegate.updated.isEmpty())
        assertEquals(0, manager.pendingCount.value)
        assertNotNull(manager.events.first() as SyncEvent.Rejected)
    }

    @Test
    fun deleteOfAlreadyDeletedServerRowCountsAsSuccess() = runTest {
        val delegate = FakeTransactionRepository().apply { deleteError = AppException.NotFoundException() }
        val (manager, _) = fixture(delegate)
        manager.enqueue(
            OfflineOperation.DeleteTransaction("delete-1", 1L, transactionId = 42L, clientId = null)
        )

        manager.processQueue()

        assertEquals(42L, delegate.deleted.single())
        assertEquals(0, manager.pendingCount.value)
    }
}
