package com.budgeyet.core.monitoring

import kotlin.js.JsModule
import kotlin.js.JsNonModule

@JsModule("@sentry/browser")
@JsNonModule
external object SentryBrowser {
    fun init(options: dynamic)
    fun captureException(exception: Throwable)
    fun configureScope(callback: (scope: dynamic) -> Unit)
    fun addBreadcrumb(breadcrumb: dynamic)
}
