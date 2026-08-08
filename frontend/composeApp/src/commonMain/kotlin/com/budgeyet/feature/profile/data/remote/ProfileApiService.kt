package com.budgeyet.feature.profile.data.remote

import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.network.apiUrl
import com.budgeyet.core.network.dto.HouseholdResponseDto
import com.budgeyet.core.network.dto.MemberRoleDto
import com.budgeyet.core.network.dto.UserResponseDto
import com.budgeyet.core.network.safeApiCall
import com.budgeyet.feature.profile.data.remote.dto.HouseholdUpdateRequestDto
import com.budgeyet.feature.profile.data.remote.dto.InviteCreateRequestDto
import com.budgeyet.feature.profile.data.remote.dto.InviteResponseDto
import com.budgeyet.feature.profile.data.remote.dto.MemberRoleUpdateRequestDto
import com.budgeyet.feature.profile.data.remote.dto.UpdateProfileRequestDto
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

// Thin wrapper over the shared HttpClient, mirroring Auth/Category/Transaction/DashboardApiService's
// shape — one function per endpoint, mapped through safeApiCall.
class ProfileApiService(private val httpClient: HttpClient) {

    suspend fun getMe(config: BackendConfig, accessToken: String): UserResponseDto = safeApiCall {
        httpClient.get(config.apiUrl("/users/me")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun updateMe(
        config: BackendConfig,
        accessToken: String,
        request: UpdateProfileRequestDto
    ): UserResponseDto = safeApiCall {
        httpClient.patch(config.apiUrl("/users/me")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getHousehold(
        config: BackendConfig,
        accessToken: String,
        householdId: Long
    ): HouseholdResponseDto = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun updateHousehold(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        request: HouseholdUpdateRequestDto
    ): HouseholdResponseDto = safeApiCall {
        httpClient.patch(config.apiUrl("/households/$householdId")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // Admin-only on the backend (require_admin_membership) — see RealProfileRepository.fetchHousehold
    // for how a 403 here (a plain Member) is handled.
    suspend fun listInvites(
        config: BackendConfig,
        accessToken: String,
        householdId: Long
    ): List<InviteResponseDto> = safeApiCall {
        httpClient.get(config.apiUrl("/households/$householdId/invites")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun createInvite(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        email: String?
    ): InviteResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/households/$householdId/invites")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(InviteCreateRequestDto(email = email))
        }.body()
    }

    suspend fun revokeInvite(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        inviteId: Long
    ) {
        safeApiCall {
            httpClient.delete(config.apiUrl("/households/$householdId/invites/$inviteId")) {
                bearerAuth(accessToken)
            }
        }
    }

    suspend fun updateMemberRole(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        memberId: Long,
        role: MemberRoleDto
    ) {
        safeApiCall {
            httpClient.patch(config.apiUrl("/households/$householdId/members/$memberId/role")) {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(MemberRoleUpdateRequestDto(role = role))
            }
        }
    }

    suspend fun removeMember(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        memberId: Long
    ) {
        safeApiCall {
            httpClient.delete(config.apiUrl("/households/$householdId/members/$memberId")) {
                bearerAuth(accessToken)
            }
        }
    }
}
