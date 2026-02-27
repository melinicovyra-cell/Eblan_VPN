package com.eblanvpn.app.data.model

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR;

    val isActive: Boolean
        get() = this == CONNECTING || this == CONNECTED
}

data class TrafficStats(
    val totalUpload: Long = 0L,
    val totalDownload: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L
) {
    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
                bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
                else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec < 1024 -> "$bytesPerSec B/s"
                bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024.0)
                bytesPerSec < 1024 * 1024 * 1024 -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024))
                else -> "%.2f GB/s".format(bytesPerSec / (1024.0 * 1024 * 1024))
            }
        }
    }
}
