package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

enum class LogLevel {
    INFO,
    DNS,
    TCP,
    TLS,
    AUTH,
    TUNNEL,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    DISCONNECTED,
    WARNING,
    ERROR,
    TRAFFIC
}
