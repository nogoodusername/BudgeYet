package com.famex.feature.transaction.domain

import com.famex.core.model.Transaction

interface TransactionRepository {
    suspend fun getTransactions(): List<Transaction>
    suspend fun getTransaction(transactionId: Long): Transaction?
}
