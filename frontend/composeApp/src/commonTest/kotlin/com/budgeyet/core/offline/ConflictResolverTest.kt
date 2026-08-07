package com.budgeyet.core.offline

import com.budgeyet.core.network.AppException
import kotlin.test.Test
import kotlin.test.assertEquals

class ConflictResolverTest {

    @Test
    fun transientAndServerSidedErrorsKeepTheOperationQueued() {
        val retryLater = listOf(
            AppException.NetworkException(),
            AppException.TimeoutException(),
            AppException.ServerException(500),
            AppException.RateLimitException(),
            AppException.AuthenticationException()
        )
        for (error in retryLater) {
            assertEquals(
                SyncResolution.RETRY_LATER,
                ConflictResolver.resolve(error),
                "expected RETRY_LATER for ${error::class.simpleName}"
            )
        }
    }

    @Test
    fun businessRejectionsDiscardTheOperation() {
        val discard = listOf(
            AppException.ConflictException(),
            AppException.ValidationException(),
            AppException.PermissionDeniedException(),
            AppException.UnknownException()
        )
        for (error in discard) {
            assertEquals(
                SyncResolution.DISCARD,
                ConflictResolver.resolve(error),
                "expected DISCARD for ${error::class.simpleName}"
            )
        }
    }

    @Test
    fun notFoundDiscardsTooBecauseDesiredEndStateIsAlreadyReached() {
        assertEquals(SyncResolution.DISCARD, ConflictResolver.resolve(AppException.NotFoundException()))
    }

    @Test
    fun unknownThrowablesAreDiscarded() {
        assertEquals(SyncResolution.DISCARD, ConflictResolver.resolve(IllegalStateException("boom")))
    }
}
