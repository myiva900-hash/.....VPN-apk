package com.example.model

enum class ConnectionMode(
    val title: String,
    val subtitle: String,
    val description: String
) {
    DIRECT(
        title = "Direct Connection",
        subtitle = "Internet",
        description = "Direct routing through selected VPN server without payload encapsulation"
    ),
    CUSTOM_PAYLOAD(
        title = "Custom Payload",
        subtitle = "TCP Mode",
        description = "Encapsulate traffic using custom HTTP/TCP injection headers and payloads"
    ),
    CUSTOM_HOST_HEADER(
        title = "Custom Host Header",
        subtitle = "HTTP Mode",
        description = "Rewrite HTTP host request headers for targeted network routing"
    ),
    CUSTOM_SNI(
        title = "Custom SNI",
        subtitle = "SSL, TLS Mode",
        description = "Custom Server Name Indication for secure TLS/SSL handshakes"
    ),
    DNS_TUNNEL(
        title = "DNSTT Tunnel",
        subtitle = "DNS Mode",
        description = "Tunnel TCP/UDP traffic over DNS queries using custom Nameserver (NS)"
    ),
    IMPORTED_CONFIG(
        title = "Imported Config",
        subtitle = ".MYVPN File",
        description = "Load exported configuration file with customized parameters and settings"
    )
}
