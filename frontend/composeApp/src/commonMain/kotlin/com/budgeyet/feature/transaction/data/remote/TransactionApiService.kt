package com.budgeyet.feature.transaction.data.remote

import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.network.apiUrl
import com.budgeyet.core.network.safeApiCall
import com.budgeyet.feature.transaction.data.remote.dto.TransactionCreateRequestDto
import com.budgeyet.feature.transaction.data.remote.dto.TransactionPageDto
import com.budgeyet.feature.transaction.data.remote.dto.TransactionResponseDto
import com.budgeyet.feature.transaction.data.remote.dto.TransactionUpdateRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Backend page size cap (TransactionFilterParams.limit, Field(le=200)) — see listAllTransactions.
private const val MAX_PAGE_SIZE = 200

// Thin wrapper over the shared HttpClient, mirroring AuthApiService/CategoryApiService's shape —
// one function per endpoint under /households/{householdId}/transactions, mapped through
// safeApiCall.
class TransactionApiService(private val httpClient: HttpClient) {

    suspend fun listTransactions(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        categoryId: Long? = null,
        limit: Int = MAX_PAGE_SIZE,
        offset: Int = 0
    ): TransactionPageDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/transactions")) {
            bearerAuth(accessToken)
            parameter("limit", limit)
            parameter("offset", offset)
            if (categoryId != null) parameter("category_id", categoryId)
        }.body()
    }

    // TransactionRepository.getTransactions()/reassignCategory take no pagination — the History
    // screen filters/groups the full list client-side (see HistoryController.load) — so this
    // pages through the backend's 200-row cap until every matching transaction is collected.
    // Capped at MAX_PAGES as a safety net against a runaway loop if `total` were ever wrong, not
    // because a household is expected to hit it.
    suspend fun listAllTransactions(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        categoryId: Long? = null
    ): List<TransactionResponseDto> {
        val all = mutableListOf<TransactionResponseDto>()
        var offset = 0
        var pages = 0
        while (pages < MAX_PAGES) {
            val page = listTransactions(config, accessToken, householdId, categoryId, MAX_PAGE_SIZE, offset)
            all += page.items
            pages++
            if (page.items.isEmpty() || all.size >= page.total) break
            offset += MAX_PAGE_SIZE
        }
        return all
    }

    suspend fun getTransaction(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        transactionId: Long
    ): TransactionResponseDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/transactions/$transactionId")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun createTransaction(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        request: TransactionCreateRequestDto
    ): TransactionResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/households/$householdId/transactions")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateTransaction(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        transactionId: Long,
        request: TransactionUpdateRequestDto
    ): TransactionResponseDto = safeApiCall {
        httpClient.patch(config.apiUrl("/households/$householdId/transactions/$transactionId")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteTransaction(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        transactionId: Long
    ) {
        safeApiCall {
            httpClient.delete(config.apiUrl("/households/$householdId/transactions/$transactionId")) {
                bearerAuth(accessToken)
            }
        }
    }
}

private const val MAX_PAGES = 25
