package com.example.model

enum class VpnConnectionState(val displayName: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    AUTHENTICATING("Authenticating..."),
    ESTABLISHING_TUNNEL("Establishing Tunnel..."),
    CONNECTED("Connected"),
    DISCONNECTING("Disconnecting..."),
    FAILED("Connection Failed")
}
