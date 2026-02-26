package com.eblanvpn.app.service

import android.net.TrafficStats
import com.eblanvpn.app.data.model.TrafficStats as AppTrafficStats
import kotlinx.coroutines.*

/**
 * Monitors network traffic using Android TrafficStats API.
 * Falls back to UID-based stats when process stats are unavailable.
 */
class TrafficMonitor(
    private val scope: CoroutineScope,
    private val onUpdate: (AppTrafficStats) -> Unit
) {
    private var job: Job? = null

    private var startTxBytes = 0L
    private var startRxBytes = 0L
    private var prevTxBytes = 0L
    private var prevRxBytes = 0L

    private val uid = android.os.Process.myUid()

    fun start() {
        stop()
        startTxBytes = getUidTx()
        startRxBytes = getUidRx()
        prevTxBytes = startTxBytes
        prevRxBytes = startRxBytes

        job = scope.launch {
            while (isActive) {
                delay(1000L)
                val tx = getUidTx()
                val rx = getUidRx()

                val uploadSpeed = (tx - prevTxBytes).coerceAtLeast(0L)
                val downloadSpeed = (rx - prevRxBytes).coerceAtLeast(0L)

                onUpdate(
                    AppTrafficStats(
                        totalUpload = (tx - startTxBytes).coerceAtLeast(0L),
                        totalDownload = (rx - startRxBytes).coerceAtLeast(0L),
                        uploadSpeed = uploadSpeed,
                        downloadSpeed = downloadSpeed
                    )
                )

                prevTxBytes = tx
                prevRxBytes = rx
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun reset() {
        startTxBytes = getUidTx()
        startRxBytes = getUidRx()
        prevTxBytes = startTxBytes
        prevRxBytes = startRxBytes
    }

    private fun getUidTx(): Long {
        val bytes = TrafficStats.getUidTxBytes(uid)
        return if (bytes == TrafficStats.UNSUPPORTED.toLong()) 0L else bytes
    }

    private fun getUidRx(): Long {
        val bytes = TrafficStats.getUidRxBytes(uid)
        return if (bytes == TrafficStats.UNSUPPORTED.toLong()) 0L else bytes
    }
}
