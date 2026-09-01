package com.budgeyet.core.monitoring

/**
 * Web/JS implementation backed by @sentry/browser (the Sentry Kotlin Multiplatform SDK only ships a
 * no-op stub for the JS target at this version, so the browser SDK is used directly via interop).
 */
object SentryBrowserMonitoring : Monitoring {
    override fun init(dsn: String, environment: String, release: String) {
        val options = js("({})")
        options.dsn = dsn
        options.environment = environment
        options.release = release
        options.tracesSampleRate = 1.0
        options.sendDefaultPii = true
        SentryBrowser.init(options)
    }

    override fun setUser(id: String?, email: String?) {
        SentryBrowser.configureScope { scope ->
            val user = js("({})")
            user.id = id
            user.email = email
            scope.setUser(user)
        }
    }

    override fun captureException(throwable: Throwable) {
        SentryBrowser.captureException(throwable)
    }

    override fun logBreadcrumb(message: String) {
        val crumb = js("({})")
        crumb.message = message
        crumb.level = "info"
        SentryBrowser.addBreadcrumb(crumb)
    }
}
