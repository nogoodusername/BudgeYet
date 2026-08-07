package com.budgeyet.core.offline

import com.budgeyet.core.cache.LocalFileStorage
import com.budgeyet.core.model.Transaction
import com.budgeyet.core.model.User
import com.budgeyet.feature.transaction.domain.TransactionRepository

// In-memory LocalFileStorage for offline tests — deterministic, no platform file I/O.
class InMemoryLocalFileStorage : LocalFileStorage {
    private val store = mutableMapOf<String, String>()
    override suspend fun readString(key: String): String? = store[key]
    override suspend fun writeString(key: String, value: String) { store[key] = value }
    override suspend fun remove(key: String) { store.remove(key) }
    override suspend fun clear() { store.clear() }
}

// Programmable TransactionRepository for SyncManager / OfflineFirstTransactionRepository tests.
// Each write records its input and can be made to throw a specific error to simulate a
// server-side rejection or a connectivity failure.
class FakeTransactionRepository : TransactionRepository {
    val added = mutableListOf<Transaction>()
    val updated = mutableListOf<Transaction>()
    val deleted = mutableListOf<Long>()

    var addError: Throwable? = null
    var updateError: Throwable? = null
    var deleteError: Throwable? = null
    var getError: Throwable? = null
    var reassignError: Throwable? = null

    private var nextId = 1_000L
    private val transactions = mutableListOf<Transaction>()

    override suspend fun getTransactions(): List<Transaction> {
        getError?.let { throw it }
        return transactions.toList()
    }
    override suspend fun getTransaction(transactionId: Long): Transaction? =
        transactions.find { it.id == transactionId }

    override suspend fun addTransaction(transaction: Transaction): Transaction {
        addError?.let { throw it }
        val saved = transaction.copy(id = nextId++)
        added += saved
        transactions += saved
        return saved
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction {
        updateError?.let { throw it }
        updated += transaction
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) transactions[index] = transaction
        return transaction
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        deleted += transactionId
        deleteError?.let { throw it }
        transactions.removeAll { it.id == transactionId }
    }

    override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long, toCategoryName: String) {
        reassignError?.let { throw it }
    }
}

fun testTransaction(id: Long, clientId: String? = null, merchant: String = "Test") = Transaction(
    id = id,
    merchant = merchant,
    amount = 10.0,
    categoryId = null,
    categoryName = null,
    paidBy = User(id = 1, email = "a@b.com", fullName = "A", nickname = "A"),
    clientId = clientId,
    transactionDateText = "Aug 5, 2026",
    createdAtText = "Aug 5, 2026"
)
