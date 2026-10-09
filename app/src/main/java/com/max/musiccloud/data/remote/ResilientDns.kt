package com.max.musiccloud.data.remote

import android.util.Log
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Resilient DNS provider that tries standard system DNS first,
 * with fallback to Google's official Anycast IPs for *.googleapis.com
 * to prevent UnknownHostException during network switches, app wake-ups, or ISP DNS hiccups.
 */
object ResilientDns : Dns {
    private const val TAG = "ResilientDns"

    private val GOOGLE_APIS_IPS = listOf(
        byteArrayOf(142.toByte(), 251.toByte(), 127.toByte(), 95.toByte()),
        byteArrayOf(172.toByte(), 217.toByte(), 16.toByte(), 202.toByte()),
        byteArrayOf(142.toByte(), 250.toByte(), 74.toByte(), 202.toByte()),
        byteArrayOf(172.toByte(), 217.toByte(), 22.toByte(), 42.toByte())
    )

    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                return addresses
            }
        } catch (e: Exception) {
            Log.w(TAG, "System DNS lookup failed for $hostname: ${e.message}")
        }

        if (hostname.endsWith("googleapis.com")) {
            Log.i(TAG, "Using Anycast fallback DNS for $hostname")
            return try {
                GOOGLE_APIS_IPS.map { ipBytes ->
                    InetAddress.getByAddress(hostname, ipBytes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating fallback InetAddress: ${e.message}")
                throw UnknownHostException("Unable to resolve host \"$hostname\"")
            }
        }

        throw UnknownHostException("Unable to resolve host \"$hostname\": No address associated with hostname")
    }
}
