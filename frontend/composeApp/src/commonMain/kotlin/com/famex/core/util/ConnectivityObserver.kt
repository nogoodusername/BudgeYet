package com.famex.core.util

import kotlinx.coroutines.flow.Flow

// Emits the current connectivity state (true = reachable, false = not), then a new value on
// every change. App.kt uses it to trigger SyncManager.processQueue on each offline→online
// transition. Platform actuals: ConnectivityManager NetworkCallback on Android, NWPathMonitor on
// iOS. WasmJS target is currently disabled in composeApp/build.gradle.kts, so it has no actual —
// add one (navigator.onLine + online/offline listeners) if the target is ever re-enabled.
interface ConnectivityObserver {
    fun observe(): Flow<Boolean>
}

expect fun createConnectivityObserver(): ConnectivityObserver
