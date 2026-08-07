package com.budgeyet.core.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineQueueTest {

    @Test
    fun enqueueAndReadBackPreservesPolymorphicOps() = runTestBlocking {
        val queue = OfflineQueue(InMemoryLocalFileStorage())
        val add = OfflineOperation.AddTransaction(
            id = "pending-1",
            createdAtEpochMillis = 1L,
            clientId = "c1",
            transaction = testTransaction(id = -1, clientId = "c1")
        )
        val delete = OfflineOperation.DeleteTransaction(
            id = "delete-1",
            createdAtEpochMillis = 2L,
            transactionId = 42L,
            clientId = null
        )

        queue.enqueue(add)
        queue.enqueue(delete)

        val all = queue.all()
        assertEquals(2, all.size)
        assertEquals(add, all[0])
        assertEquals(delete, all[1])
    }

    @Test
    fun removeDropsOnlyTheMatchingOp() = runTestBlocking {
        val queue = OfflineQueue(InMemoryLocalFileStorage())
        queue.enqueue(OfflineOperation.AddTransaction("a", 1L, "c1", testTransaction(-1, "c1")))
        queue.enqueue(OfflineOperation.AddTransaction("b", 2L, "c2", testTransaction(-2, "c2")))

        queue.remove("a")

        val all = queue.all()
        assertEquals(1, all.size)
        assertEquals("b", all.single().id)
        assertEquals(1, queue.count())
    }

    @Test
    fun corruptedStorageYieldsEmptyQueueInsteadOfCrashing() = runTestBlocking {
        val storage = InMemoryLocalFileStorage()
        storage.writeString(OfflineQueue.KEY, "this is not json")

        val queue = OfflineQueue(storage)
        assertTrue(queue.all().isEmpty())
        assertEquals(0, queue.count())
    }

    @Test
    fun clearRemovesEverything() = runTestBlocking {
        val queue = OfflineQueue(InMemoryLocalFileStorage())
        queue.enqueue(OfflineOperation.AddTransaction("a", 1L, "c1", testTransaction(-1, "c1")))
        queue.clear()
        assertTrue(queue.all().isEmpty())
        assertEquals(0, queue.count())
    }
}

// kotlin.test's runTest lives in kotlinx-coroutines-test; wrap to keep test signatures short.
private fun runTestBlocking(block: suspend () -> Unit) =
    kotlinx.coroutines.test.runTest { block() }
