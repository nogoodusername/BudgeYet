package com.famex.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.famex.core.persistence.AndroidAppContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// ConnectivityManager-backed observer. The manifest already declares ACCESS_NETWORK_STATE
// (androidMain/AndroidManifest.xml), so registering a callback is allowed without extra setup.
private class AndroidConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<Boolean> = callbackFlow {
        val cm = AndroidAppContext.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun currentReachable(): Boolean {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onUnavailable() { trySend(false) }
        }

        trySend(currentReachable())
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}

actual fun createConnectivityObserver(): ConnectivityObserver = AndroidConnectivityObserver()
