package com.budgeyet.core.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

// Network.framework monitor (Kotlin/Native 1.9.23 exposes the C API — nw_path_monitor_* — not the
// ObjC NWPathMonitor class). set_update_handler fires immediately with the current path and again
// on every change, so there's no separate "initial state" read needed (unlike the Android actual).
@OptIn(ExperimentalForeignApi::class)
private class IosConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        if (monitor == null) {
            close(IllegalStateException("nw_path_monitor_create returned null"))
            return@callbackFlow
        }
        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(nw_path_get_status(path) == nw_path_status_satisfied)
        }
        val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}

actual fun createConnectivityObserver(): ConnectivityObserver = IosConnectivityObserver()
