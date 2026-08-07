package com.budgeyet.core.util

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private class JsConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<Boolean> = callbackFlow {
        trySend(js("navigator.onLine") as Boolean)
        val onOnline: (dynamic) -> Unit = { trySend(true) }
        val onOffline: (dynamic) -> Unit = { trySend(false) }
        window.addEventListener("online", onOnline)
        window.addEventListener("offline", onOffline)
        awaitClose {
            window.removeEventListener("online", onOnline)
            window.removeEventListener("offline", onOffline)
        }
    }
}

actual fun createConnectivityObserver(): ConnectivityObserver = JsConnectivityObserver()