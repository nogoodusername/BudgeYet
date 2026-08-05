package com.famex.core.network

import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

// Wraps a single Ktor call and maps every failure mode (unreachable host, timeout, non-2xx
// response, malformed body) to a typed AppException. The shared client (HttpClientFactory) sets
// expectSuccess = true, so any non-2xx response surfaces here as a ResponseException rather than
// a plain HttpResponse the caller has to status-check manually.
suspend fun <T> safeApiCall(block: suspend () -> T): T {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        throw e.toAppException()
    } catch (e: HttpRequestTimeoutException) {
        throw AppException.TimeoutException()
    } catch (e: IOException) {
        throw AppException.NetworkException()
    } catch (e: SerializationException) {
        throw AppException.UnknownException("Unexpected response from server")
    }
}

private suspend fun ResponseException.toAppException(): AppException {
    val status = response.status
    val detail = extractDetail()
    return when (status) {
        HttpStatusCode.Unauthorized -> detail?.let { AppException.AuthenticationException(it) } ?: AppException.AuthenticationException()
        HttpStatusCode.Forbidden -> detail?.let { AppException.PermissionDeniedException(it) } ?: AppException.PermissionDeniedException()
        HttpStatusCode.NotFound -> detail?.let { AppException.NotFoundException(it) } ?: AppException.NotFoundException()
        HttpStatusCode.Conflict -> detail?.let { AppException.ConflictException(it) } ?: AppException.ConflictException()
        HttpStatusCode.UnprocessableEntity -> detail?.let { AppException.ValidationException(it) } ?: AppException.ValidationException()
        HttpStatusCode.TooManyRequests -> detail?.let { AppException.RateLimitException(it) } ?: AppException.RateLimitException()
        else -> if (status.value in 500..599) {
            detail?.let { AppException.ServerException(status.value, it) } ?: AppException.ServerException(status.value)
        } else {
            AppException.UnknownException(detail ?: "Something went wrong (${status.value}).")
        }
    }
}

// Backend error bodies come in two shapes: {"detail": "message"} for the domain AppError
// hierarchy (see backend/app/main.py _ERROR_STATUS_CODES — NotFoundError, ConflictError,
// AuthenticationError, etc. all serialize their `.message` this way), or
// {"detail": [{"msg": "...", ...}, ...]} for raw FastAPI/pydantic request-validation failures
// that never reach a service (malformed request body/query params). Handle both instead of
// assuming one.
private suspend fun ResponseException.extractDetail(): String? = try {
    val bodyText: String = response.body()
    val element = Json { ignoreUnknownKeys = true }.parseToJsonElement(bodyText)
    when (val detail = element.jsonObject["detail"]) {
        is JsonPrimitive -> detail.content
        is JsonArray -> detail
            .mapNotNull { item -> (item.jsonObject["msg"] as? JsonPrimitive)?.content }
            .joinToString("; ")
            .ifBlank { null }
        else -> null
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}
