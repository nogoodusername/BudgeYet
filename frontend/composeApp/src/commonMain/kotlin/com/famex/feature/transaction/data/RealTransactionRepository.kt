package com.famex.feature.transaction.data

import com.famex.core.model.Transaction
import com.famex.core.network.AppException
import com.famex.core.network.HouseholdRequestContextProvider
import com.famex.core.network.mapper.toDto
import com.famex.feature.transaction.data.mapper.toDomain
import com.famex.feature.transaction.data.mapper.toDto
import com.famex.feature.transaction.data.remote.TransactionApiService
import com.famex.feature.transaction.data.remote.dto.TransactionCreateRequestDto
import com.famex.feature.transaction.data.remote.dto.TransactionUpdateRequestDto
import com.famex.feature.transaction.domain.TransactionRepository

// Real, network-backed TransactionRepository — same shape as RealCategoryRepository: the
// interface carries no household id, so every call resolves one via HouseholdRequestContextProvider.
class RealTransactionRepository(
    private val api: TransactionApiService,
    private val contextProvider: HouseholdRequestContextProvider
) : TransactionRepository {

    override suspend fun getTransactions(): List<Transaction> {
        val (config, token, householdId) = contextProvider.get()
        return api.listAllTransactions(config, token, householdId).map { it.toDomain() }
    }

    override suspend fun getTransaction(transactionId: Long): Transaction? {
        val (config, token, householdId) = contextProvider.get()
        return try {
            api.getTransaction(config, token, householdId, transactionId).toDomain()
        } catch (e: AppException.NotFoundException) {
            null
        }
    }

    override suspend fun addTransaction(transaction: Transaction): Transaction {
        val (config, token, householdId) = contextProvider.get()
        val request = TransactionCreateRequestDto(
            amount = transaction.amount,
            merchant = transaction.merchant,
            type = transaction.type.toDto(),
            paymentMode = transaction.paymentMode.toDto(),
            notes = transaction.notes,
            transactionDate = "${transaction.transactionDate}T00:00:00",
            categoryId = transaction.categoryId,
            paidById = transaction.paidBy.id
        )
        return api.createTransaction(config, token, householdId, request).toDomain()
    }

    override suspend fun updateTransaction(transaction: Transaction): Transaction {
        val (config, token, householdId) = contextProvider.get()
        val request = TransactionUpdateRequestDto(
            amount = transaction.amount,
            merchant = transaction.merchant,
            type = transaction.type.toDto(),
            categoryId = transaction.categoryId,
            paidById = transaction.paidBy.id,
            paymentMode = transaction.paymentMode.toDto(),
            notes = transaction.notes,
            transactionDate = "${transaction.transactionDate}T00:00:00"
        )
        return api.updateTransaction(config, token, householdId, transaction.id, request).toDomain()
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        val (config, token, householdId) = contextProvider.get()
        api.deleteTransaction(config, token, householdId, transactionId)
    }

    // No bulk-move endpoint — list every transaction currently in fromCategoryId (paginated
    // server-side, see TransactionApiService.listAllTransactions) and PATCH each one's
    // category_id individually. toCategoryName is unused: the backend derives the category's
    // name from category_id itself, it's only there for FakeTransactionRepository's in-memory
    // denormalized copy.
    override suspend fun reassignCategory(fromCategoryId: Long, toCategoryId: Long, toCategoryName: String) {
        val (config, token, householdId) = contextProvider.get()
        val toReassign = api.listAllTransactions(config, token, householdId, categoryId = fromCategoryId)
        for (dto in toReassign) {
            api.updateTransaction(
                config,
                token,
                householdId,
                dto.id,
                TransactionUpdateRequestDto(categoryId = toCategoryId)
            )
        }
    }
}
