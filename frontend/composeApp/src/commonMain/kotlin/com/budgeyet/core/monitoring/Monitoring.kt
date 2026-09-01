package com.budgeyet.core.monitoring

/**
 * Provider-agnostic monitoring surface. Feature code should depend only on this interface so the
 * underlying SDK (Sentry) can be swapped without touching call sites.
 */
interface Monitoring {
    /** Initialize the SDK. Safe to call once at app start. */
    fun init(dsn: String, environment: String, release: String)

    /** Associate subsequent events with a user. Pass nulls to clear. */
    fun setUser(id: String?, email: String?)

    /** Report a handled/unhandled throwable. */
    fun captureException(throwable: Throwable)

    /** Drop a breadcrumb into the current event context. */
    fun logBreadcrumb(message: String)
}
