package com.budgeyet.core.network

// Typed network/API errors. This codebase reads `exception.message` directly at the
// presentation layer (see DashboardController's `catch (t: Throwable)` /
// `errorMessage = t.message`) rather than a Resource<T>/AppError wrapper, so `message` on each
// of these *is* the user-facing string — safeApiCall (SafeApiCall.kt) is what produces it.
// Kept as distinct subclasses (not a single exception with a status code) so callers that need
// to react differently — e.g. sign the user out on AuthenticationException, or avoid retrying
// ValidationException — can catch the specific type instead of branching on an int.
sealed class AppException(message: String) : Exception(message) {
    class NetworkException(
        message: String = "Unable to reach the server. Check your connection and try again."
    ) : AppException(message)

    class TimeoutException(
        message: String = "The request timed out. Please try again."
    ) : AppException(message)

    class AuthenticationException(
        message: String = "Your session has expired. Please sign in again."
    ) : AppException(message)

    class PermissionDeniedException(
        message: String = "You don't have permission to do that."
    ) : AppException(message)

    class NotFoundException(
        message: String = "Not found."
    ) : AppException(message)

    class ConflictException(
        message: String = "That conflicts with existing data."
    ) : AppException(message)

    class ValidationException(
        message: String = "Please check your input and try again."
    ) : AppException(message)

    class RateLimitException(
        message: String = "Too many attempts. Please wait and try again."
    ) : AppException(message)

    class ServerException(
        val statusCode: Int,
        message: String = "Something went wrong on the server. Please try again later."
    ) : AppException(message)

    class UnknownException(
        message: String = "Something went wrong. Please try again."
    ) : AppException(message)
}
