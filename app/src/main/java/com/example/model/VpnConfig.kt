package com.example.model

data class VpnConfig(
    val selectedServerId: String = "rw_gisozi",
    val selectedPort: Int = 80,
    val connectionMode: ConnectionMode = ConnectionMode.DIRECT,
    val isCustomSetupEnabled: Boolean = false,
    // Custom Payload parameters
    val payloadString: String = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]",
    val payloadUrl: String = "mutiganda.vpn.network",
    val payloadMethod: String = "CONNECT",
    val payloadInjection: String = "Normal",
    // Host Header & SNI parameters
    val customHostHeader: String = "cloudflare.com",
    val customSni: String = "vpn.mutigandayvan.net",
    val tlsVersion: String = "TLS 1.3",
    val enableSslProxy: Boolean = true,
    // DNS Tunnel parameters
    val dnsNameserver: String = "ns1.mutigandavpn.org",
    val dnsResolver: String = "1.1.1.1",
    val dnsPublicKey: String = "a1b2c3d4e5f67890abcdef1234567890abcdef12",
    // Imported configuration profile metadata
    val importedConfigName: String = "",
    val importedConfigNote: String = "",
    val isConfigLocked: Boolean = false
)
