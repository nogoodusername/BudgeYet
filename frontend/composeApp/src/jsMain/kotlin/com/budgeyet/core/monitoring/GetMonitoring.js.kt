package com.budgeyet.core.monitoring

actual fun getMonitoring(): Monitoring = SentryBrowserMonitoring
