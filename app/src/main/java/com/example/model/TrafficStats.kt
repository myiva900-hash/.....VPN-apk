package com.example.model

import java.util.Locale

data class TrafficStats(
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val downloadSpeedBps: Long = 0L
) {
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun formatSpeed(bps: Long): String {
        return when {
            bps < 1024 -> "$bps B/s"
            bps < 1024 * 1024 -> String.format(Locale.US, "%.1f KB/s", bps / 1024.0)
            else -> String.format(Locale.US, "%.1f MB/s", bps / (1024.0 * 1024.0))
        }
    }

    val uploadFormatted: String get() = formatBytes(uploadBytes)
    val downloadFormatted: String get() = formatBytes(downloadBytes)
    val uploadSpeedFormatted: String get() = formatSpeed(uploadSpeedBps)
    val downloadSpeedFormatted: String get() = formatSpeed(downloadSpeedBps)
}
