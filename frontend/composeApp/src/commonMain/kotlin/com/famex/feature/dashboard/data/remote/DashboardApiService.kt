package com.famex.feature.dashboard.data.remote

import com.famex.core.model.BackendConfig
import com.famex.core.network.apiUrl
import com.famex.core.network.dto.HouseholdResponseDto
import com.famex.core.network.safeApiCall
import com.famex.feature.dashboard.data.remote.dto.ActivityFeedPageDto
import com.famex.feature.dashboard.data.remote.dto.DashboardResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter

// Thin wrapper over the shared HttpClient, mirroring Auth/Category/TransactionApiService's shape.
class DashboardApiService(private val httpClient: HttpClient) {

    suspend fun getDashboard(
        config: BackendConfig,
        accessToken: String,
        householdId: Long
    ): DashboardResponseDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/dashboard")) {
            bearerAuth(accessToken)
        }.body()
    }

    // DashboardData.household needs the full household (all members) — GET .../dashboard
    // doesn't return one at all (see backend/app/schemas/dashboard.py DashboardResponse), so this
    // is the same GET /households/{id} call AuthApiService.getHousehold already makes, reusing
    // the shared HouseholdResponseDto (core/network/dto/HouseholdDto.kt) rather than depending on
    // the auth feature's API service.
    suspend fun getHousehold(
        config: BackendConfig,
        accessToken: String,
        householdId: Long
    ): HouseholdResponseDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun getActivityFeed(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        limit: Int,
        offset: Int = 0
    ): ActivityFeedPageDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/activity-feed")) {
            bearerAuth(accessToken)
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()
    }
}
