package com.trackr.service

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owned by the foreground service. Only a validated default network can wake a failed ride. */
internal class TrackingNetworkRecovery(private val connectivity: ConnectivityManager?) {
    private val _recoveries = MutableStateFlow(0L)
    val recoveries = _recoveries.asStateFlow()

    // start, close and the callbacks all run on Main.
    private var registered = false
    private var defaultNetwork: Network? = null
    private var validated = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (registered) defaultNetwork = network
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!registered || network != defaultNetwork) return
            val nowValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (nowValidated && !validated) _recoveries.value++
            validated = nowValidated
        }

        override fun onLost(network: Network) {
            if (!registered || network != defaultNetwork) return
            defaultNetwork = null
            validated = false
        }
    }

    fun start() {
        val manager = connectivity ?: return
        if (registered) return
        registered = runCatching {
            manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        }.onFailure { Log.w(TAG, "Cannot monitor connectivity; using normal tracking polls", it) }.isSuccess
    }

    fun close() {
        if (!registered) return
        registered = false
        defaultNetwork = null
        validated = false
        runCatching { connectivity?.unregisterNetworkCallback(callback) }
    }

    private companion object {
        const val TAG = "TrackingNetworkRecovery"
    }
}
