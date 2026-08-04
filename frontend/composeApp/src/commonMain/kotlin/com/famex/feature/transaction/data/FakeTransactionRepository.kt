package com.famex.feature.transaction.data

import com.famex.core.model.Transaction
import com.famex.feature.transaction.domain.TransactionRepository
import com.famex.fixtures.DummyScenario
import com.famex.fixtures.dummyTransactionHistory
import kotlinx.coroutines.delay

class FakeTransactionRepository(private val scenario: DummyScenario) : TransactionRepository {
    override suspend fun getTransactions(): List<Transaction> {
        delay(400)
        return dummyTransactionHistory(scenario)
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? {
        delay(300)
        return dummyTransactionHistory(scenario).find { it.id == transactionId }
    }
}
