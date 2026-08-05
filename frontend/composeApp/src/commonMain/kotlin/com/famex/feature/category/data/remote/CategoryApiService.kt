package com.famex.feature.category.data.remote

import com.famex.core.model.BackendConfig
import com.famex.core.network.apiUrl
import com.famex.core.network.dto.CategoryWithStatsDto
import com.famex.core.network.safeApiCall
import com.famex.feature.category.data.remote.dto.CategoryCreateRequestDto
import com.famex.feature.category.data.remote.dto.CategoryResponseDto
import com.famex.feature.category.data.remote.dto.CategoryUpdateRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Thin wrapper over the shared HttpClient, mirroring AuthApiService's shape — one function per
// endpoint under /households/{householdId}/categories, mapped through safeApiCall.
class CategoryApiService(private val httpClient: HttpClient) {

    suspend fun listCategories(
        config: BackendConfig,
        accessToken: String,
        householdId: Long
    ): List<CategoryWithStatsDto> = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/categories")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun getCategory(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        categoryId: Long
    ): CategoryWithStatsDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/categories/$categoryId")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun createCategory(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        name: String,
        icon: String,
        monthlyLimit: Double
    ): CategoryResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/households/$householdId/categories")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(CategoryCreateRequestDto(name = name, icon = icon, monthlyLimit = monthlyLimit))
        }.body()
    }

    suspend fun updateCategoryLimit(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        categoryId: Long,
        monthlyLimit: Double
    ) {
        safeApiCall {
            httpClient.patch(config.apiUrl("/households/$householdId/categories/$categoryId")) {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(CategoryUpdateRequestDto(monthlyLimit = monthlyLimit))
            }
        }
    }

    suspend fun deleteCategory(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        categoryId: Long
    ) {
        safeApiCall {
            httpClient.delete(config.apiUrl("/households/$householdId/categories/$categoryId")) {
                bearerAuth(accessToken)
            }
        }
    }
}
