package com.famex.feature.transaction.data

import com.famex.core.model.Transaction
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyTransactionHistory
import kotlinx.coroutines.delay

class FakeTransactionRepository(private val scenario: DummyScenario) : TransactionRepository {
    // In-memory only — seeded from fixtures, then grown by addTransaction so newly logged
    // expenses show up in History/Dashboard for the rest of the app session.
    private var transactions: List<Transaction> = dummyTransactionHistory(scenario)

    override suspend fun getTransactions(): List<Transaction> {
        delay(400)
        return transactions
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? {
        delay(300)
        return transactions.find { it.id == transactionId }
    }

    override suspend fun addTransaction(transaction: Transaction): Transaction {
        delay(300)
        val nextId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
        val saved = transaction.copy(id = nextId)
        transactions = listOf(saved) + transactions
        return saved
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction {
        delay(300)
        transactions = transactions.map { if (it.id == transaction.id) transaction else it }
        return transaction
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        delay(300)
        transactions = transactions.filterNot { it.id == transactionId }
    }
}
