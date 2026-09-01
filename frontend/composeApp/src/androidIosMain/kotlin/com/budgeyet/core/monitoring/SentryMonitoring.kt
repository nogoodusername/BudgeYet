package com.budgeyet.core.monitoring

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import io.sentry.kotlin.multiplatform.protocol.User

/**
 * Sentry implementation shared by the Android and iOS targets (the Sentry Kotlin Multiplatform SDK
 * exposes a common API that compiles to the native Android/iOS SDKs under the hood).
 */
object SentryMonitoring : Monitoring {
    override fun init(dsn: String, environment: String, release: String) {
        Sentry.init {
            it.dsn = dsn
            it.environment = environment
            it.release = release
            it.tracesSampleRate = 1.0
            it.sendDefaultPii = true
        }
    }

    override fun setUser(id: String?, email: String?) {
        Sentry.setUser(User().apply {
            this.id = id
            this.email = email
        })
    }

    override fun captureException(throwable: Throwable) {
        Sentry.captureException(throwable)
    }

    override fun logBreadcrumb(message: String) {
        Sentry.addBreadcrumb(Breadcrumb.info(message))
    }
}
