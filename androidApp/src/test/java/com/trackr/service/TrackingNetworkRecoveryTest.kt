package com.trackr.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TrackingNetworkRecoveryTest {
    @Test fun reportsValidatedTransitionsOnceAndUnregisters() {
        val context: Context = RuntimeEnvironment.getApplication()
        val connectivity = context.getSystemService(ConnectivityManager::class.java)!!
        val shadow = shadowOf(connectivity)
        val monitor = TrackingNetworkRecovery(connectivity)
        monitor.start()
        try {
            val callback = shadow.networkCallbacks.single()
            val first = ShadowNetwork.newInstance(10)
            val next = ShadowNetwork.newInstance(11)
            val internet = ShadowNetworkCapabilities.newInstance().also {
                shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            val validated = ShadowNetworkCapabilities.newInstance().also {
                shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            callback.onAvailable(first)
            callback.onCapabilitiesChanged(first, internet)
            assertEquals(0L, monitor.recoveries.value)
            callback.onCapabilitiesChanged(first, validated)
            callback.onCapabilitiesChanged(first, validated)
            assertEquals(1L, monitor.recoveries.value)

            callback.onAvailable(next)
            callback.onLost(first) // a seamless switch need not wake any waiting ride
            callback.onCapabilitiesChanged(next, validated)
            assertEquals(1L, monitor.recoveries.value)
            callback.onCapabilitiesChanged(next, internet)
            callback.onCapabilitiesChanged(next, validated)
            assertEquals(2L, monitor.recoveries.value)
            callback.onLost(next)
            callback.onAvailable(first)
            callback.onCapabilitiesChanged(first, validated)
            assertEquals(3L, monitor.recoveries.value)
        } finally {
            monitor.close()
        }
        assertTrue(shadow.networkCallbacks.isEmpty())
    }
}
