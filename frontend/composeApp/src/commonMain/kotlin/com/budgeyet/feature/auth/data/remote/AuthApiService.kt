package com.budgeyet.feature.auth.data.remote

import com.budgeyet.core.model.BackendConfig
import com.budgeyet.core.network.apiUrl
import com.budgeyet.core.network.dto.HouseholdMemberResponseDto
import com.budgeyet.core.network.dto.HouseholdResponseDto
import com.budgeyet.core.network.safeApiCall
import com.budgeyet.feature.auth.data.remote.dto.BudgetCreateRequestDto
import com.budgeyet.feature.auth.data.remote.dto.CategoryCreateRequestDto
import com.budgeyet.feature.auth.data.remote.dto.ForgotPinRequestDto
import com.budgeyet.feature.auth.data.remote.dto.HouseholdCreateRequestDto
import com.budgeyet.feature.auth.data.remote.dto.JoinHouseholdRequestDto
import com.budgeyet.feature.auth.data.remote.dto.LoginRequestDto
import com.budgeyet.feature.auth.data.remote.dto.LoginResponseDto
import com.budgeyet.feature.auth.data.remote.dto.SignUpRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Thin wrapper over the shared HttpClient — every function is one endpoint call, mapped through
// safeApiCall so callers (RealAuthRepository) only ever see AppException, never a raw Ktor
// exception. No response decoding beyond what the caller actually needs (e.g. signup/forgot-pin
// callers only care whether the call succeeded, so those return Unit).
class AuthApiService(private val httpClient: HttpClient) {

    suspend fun signUp(config: BackendConfig, fullName: String, nickname: String, email: String, pin: String) {
        safeApiCall {
            httpClient.post(config.apiUrl("/auth/signup")) {
                contentType(ContentType.Application.Json)
                setBody(SignUpRequestDto(email = email, fullName = fullName, nickname = nickname, pin = pin))
            }
        }
    }

    suspend fun login(config: BackendConfig, email: String, pin: String): LoginResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/auth/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email = email, pin = pin))
        }.body()
    }

    suspend fun forgotPin(config: BackendConfig, email: String) {
        safeApiCall {
            httpClient.post(config.apiUrl("/auth/forgot-pin")) {
                contentType(ContentType.Application.Json)
                setBody(ForgotPinRequestDto(email = email))
            }
        }
    }

    // Null when the signed-in user hasn't created/joined a household yet — see
    // backend/app/api/v1/endpoints/users.py GET /users/me/household.
    suspend fun getMyHousehold(config: BackendConfig, accessToken: String): HouseholdResponseDto? = safeApiCall {
        httpClient.get(config.apiUrl("/users/me/household")) {
            bearerAuth(accessToken)
        }.body()
    }

    suspend fun getHousehold(config: BackendConfig, accessToken: String, householdId: Long): HouseholdResponseDto =
        safeApiCall {
            httpClient.get(config.apiUrl("/households/$householdId")) {
                bearerAuth(accessToken)
            }.body()
        }

    suspend fun createHousehold(
        config: BackendConfig,
        accessToken: String,
        name: String,
        currency: String,
        cycleStartDay: Int
    ): HouseholdResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/households")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(
                HouseholdCreateRequestDto(
                    name = name,
                    currency = currency,
                    language = "en",
                    cycleStartDay = cycleStartDay
                )
            )
        }.body()
    }

    suspend fun joinHousehold(
        config: BackendConfig,
        accessToken: String,
        inviteToken: String
    ): HouseholdMemberResponseDto = safeApiCall {
        httpClient.post(config.apiUrl("/households/join")) {
            bearerAuth(accessToken)
            contentType(ContentType.Application.Json)
            setBody(JoinHouseholdRequestDto(token = inviteToken))
        }.body()
    }

    suspend fun createBudget(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        name: String,
        monthlyGoalAmount: Double
    ) {
        safeApiCall {
            httpClient.post(config.apiUrl("/households/$householdId/budgets")) {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(BudgetCreateRequestDto(name = name, monthlyGoalAmount = monthlyGoalAmount))
            }
        }
    }

    suspend fun createCategory(
        config: BackendConfig,
        accessToken: String,
        householdId: Long,
        name: String,
        icon: String,
        monthlyLimit: Double
    ) {
        safeApiCall {
            httpClient.post(config.apiUrl("/households/$householdId/categories")) {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(CategoryCreateRequestDto(name = name, icon = icon, monthlyLimit = monthlyLimit))
            }
        }
    }
}
