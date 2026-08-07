package com.budgeyet.core.offline

import com.budgeyet.core.cache.LocalCacheStore
import com.budgeyet.core.network.AppException
import com.budgeyet.feature.transaction.data.OfflineFirstTransactionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OfflineFirstTransactionRepositoryTest {

    private fun fixture(
        delegate: FakeTransactionRepository = FakeTransactionRepository(),
        cacheStore: LocalCacheStore = LocalCacheStore(InMemoryLocalFileStorage()),
        queue: OfflineQueue = OfflineQueue(InMemoryLocalFileStorage())
    ): Triple<OfflineFirstTransactionRepository, FakeTransactionRepository, OfflineQueue> {
        val syncManager = SyncManager(delegate, cacheStore, queue)
        return Triple(
            OfflineFirstTransactionRepository(delegate, cacheStore, queue, syncManager),
            delegate,
            queue
        )
    }

    @Test
    fun addOfflineReturnsPendingRowAndQueuesIt() = runTest {
        val (repo, delegate, queue) = fixture()
        delegate.addError = AppException.NetworkException()

        val result = repo.addTransaction(testTransaction(id = 0, merchant = "Coffee"))

        assertTrue(result.isPending)
        assertNotNull(result.clientId)
        assertTrue(result.id < 0, "pending rows get a negative temp id")
        assertEquals("Coffee", result.merchant)
        assertEquals(1, queue.count())
        val op = queue.all().single() as OfflineOperation.AddTransaction
        assertEquals(result.clientId, op.clientId)
    }

    @Test
    fun addOnlineHitsServerAndNeverQueues() = runTest {
        val (repo, delegate, queue) = fixture()

        val result = repo.addTransaction(testTransaction(id = 0, merchant = "Coffee"))

        assertEquals(1000L, result.id)
        assertFalse(result.isPending)
        assertEquals("Coffee", delegate.added.single().merchant)
        assertEquals(0, queue.count())
    }

    @Test
    fun getTransactionsOfflineMergesPendingCreatesOnTopOfCache() = runTest {
        val cacheStore = LocalCacheStore(InMemoryLocalFileStorage())
        cacheStore.cacheTransactions(listOf(testTransaction(id = 1, merchant = "Confirmed")))
        val delegate = FakeTransactionRepository().apply { getError = AppException.NetworkException() }
        val queue = OfflineQueue(InMemoryLocalFileStorage())
        val (repo, _, _) = fixture(delegate = delegate, cacheStore = cacheStore, queue = queue)
        queue.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", testTransaction(id = -1, clientId = "c1", merchant = "Pending"))
        )

        val result = repo.getTransactions()

        // Pending create surfaces first, then the cached server-confirmed row — no network call.
        assertEquals(listOf("Pending", "Confirmed"), result.map { it.merchant })
    }

    @Test
    fun getTransactionsOfflineWithoutCacheRethrowsNetworkError() = runTest {
        val delegate = FakeTransactionRepository().apply { getError = AppException.NetworkException() }
        val (repo, _, _) = fixture(delegate = delegate)

        val thrown = runCatching { repo.getTransactions() }.exceptionOrNull()

        assertTrue(thrown is AppException.NetworkException, "expected NetworkException, got $thrown")
    }

    @Test
    fun deleteOfPendingCreateDropsTheQueuedAddInsteadOfQueuingADelete() = runTest {
        val (repo, _, queue) = fixture()
        val pending = testTransaction(id = -7, clientId = "c1", merchant = "Pending")
        queue.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", pending)
        )
        // Don't let the delete reach the server — it's a pending row that never existed there.
        repo.deleteTransaction(-7)

        assertEquals(0, queue.count())
    }

    @Test
    fun updateOfflineQueuesUpdateAndOptimisticallyUpdatesCache() = runTest {
        val tx = testTransaction(id = 5, merchant = "Original")
        val cacheStore = LocalCacheStore(InMemoryLocalFileStorage())
        cacheStore.cacheTransactions(listOf(tx))
        val delegate = FakeTransactionRepository().apply { updateError = AppException.NetworkException() }
        val (repo, _, queue) = fixture(delegate = delegate, cacheStore = cacheStore)
        val edited = tx.copy(merchant = "Edited")

        val result = repo.updateTransaction(edited)

        assertEquals(1, queue.count())
        val op = queue.all().single() as OfflineOperation.UpdateTransaction
        assertEquals(5L, op.transactionId)
        assertEquals("Edited", result.merchant)
        // Cache shows the optimistic edit.
        assertEquals("Edited", cacheStore.getCachedTransactions().orEmpty().single().merchant)
    }

    @Test
    fun getTransactionReturnsPendingRowByTempId() = runTest {
        val (repo, _, queue) = fixture()
        val pending = testTransaction(id = -9, clientId = "c1", merchant = "Pending")
        queue.enqueue(
            OfflineOperation.AddTransaction("pending-1", 1L, "c1", pending)
        )

        val found = repo.getTransaction(-9)

        assertEquals("Pending", found?.merchant)
        assertTrue(found?.isPending == true)
    }

    @Test
    fun reassignCategoryIsNotQueuedAndPropagatesNetworkError() = runTest {
        val delegate = FakeTransactionRepository().apply { reassignError = AppException.NetworkException() }
        val queue = OfflineQueue(InMemoryLocalFileStorage())
        val (repo, _, _) = fixture(delegate = delegate, queue = queue)

        val thrown = runCatching { repo.reassignCategory(fromCategoryId = 1, toCategoryId = 2, toCategoryName = "X") }
            .exceptionOrNull()

        // Not queued — category writes are deliberately out of the offline-write scope (AGENTS.md).
        assertEquals(0, queue.count())
        assertTrue(thrown is AppException.NetworkException, "expected NetworkException, got $thrown")
    }
}
