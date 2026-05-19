package com.privacyshield.remote.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.privacyshield.remote.model.DeviceType
import com.privacyshield.remote.model.RemoteDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Discovers Android TV / Google TV devices on the local network using mDNS.
 * Service type: _androidtvremote2._tcp.
 *
 * On Android 12+ (API 31), mDNS works without CHANGE_WIFI_MULTICAST_STATE if NsdManager
 * handles it internally. On older devices, acquiring a MulticastLock before calling
 * startDiscovery() improves reliability.
 */
class NetworkDiscovery(private val context: Context) {

    companion object {
        const val SERVICE_TYPE_ANDROID_TV = "_androidtvremote2._tcp."
        const val SERVICE_TYPE_GOOGLERPC = "_googlerpc._tcp."
    }

    /**
     * Returns a cold Flow of discovered [RemoteDevice] objects.
     * Collecting this Flow starts mDNS discovery; cancelling the collector stops it.
     */
    fun discoverAndroidTvDevices(): Flow<RemoteDevice> = callbackFlow {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: run { close(IllegalStateException("NsdManager not available")); return@callbackFlow }

        val isDiscovering = AtomicBoolean(false)

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                isDiscovering.set(true)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering.set(false)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(Exception("NSD start failed, error=$errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                // Not fatal — flow is already closing
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Create a fresh listener per resolution (NsdManager requirement)
                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        // Resolution failed — skip this service
                    }

                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val host = info.host?.hostAddress ?: return
                        val device = RemoteDevice(
                            id = "${info.serviceName}@$host:${info.port}",
                            name = sanitizeName(info.serviceName),
                            host = host,
                            port = info.port,
                            type = guessDeviceType(info)
                        )
                        trySend(device)
                    }
                }
                try {
                    nsdManager.resolveService(serviceInfo, resolveListener)
                } catch (_: Exception) {
                    // Ignore — discovery is best-effort
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                // Could emit a "removed" event; for now we just let it be
            }
        }

        try {
            nsdManager.discoverServices(
                SERVICE_TYPE_ANDROID_TV,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            if (isDiscovering.get()) {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (_: Exception) {}
            }
        }
    }

    private fun sanitizeName(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9 \\-_]"), "").trim().ifBlank { "Unknown TV" }

    private fun guessDeviceType(info: NsdServiceInfo): DeviceType {
        val name = info.serviceName.lowercase()
        return when {
            name.contains("fire") || name.contains("amazon") -> DeviceType.FIRE_TV
            else -> DeviceType.ANDROID_TV
        }
    }
}
