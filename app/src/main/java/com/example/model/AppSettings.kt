package com.example.model

data class AppSettings(
    // Notifications
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    // Appearance
    val theme: String = "Dark", // "System Default", "Light", "Dark"
    // Connection
    val connectionRetry: Boolean = true,
    val forwardDns: Boolean = false,
    val customDns: Boolean = false,
    val primaryDns: String = "1.1.1.1",
    val secondaryDns: String = "1.0.0.1",
    val connectionTimeoutSeconds: Int = 30,
    val autoReconnect: Boolean = true
)
