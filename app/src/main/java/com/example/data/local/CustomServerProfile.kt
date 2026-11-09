package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.VpnServer

@Entity(tableName = "custom_server_profiles")
data class CustomServerProfile(
    @PrimaryKey
    val id: String = "custom_${System.currentTimeMillis()}",
    val serverName: String,
    val hostOrIp: String,
    val port: Int,
    val username: String = "",
    val encryptedPassword: String = "", // Protected by Android Keystore AES-256 GCM
    val vpnProtocol: String = "OpenVPN", // OpenVPN, WireGuard, SSH, Direct TLS, Shadowsocks
    val location: String = "Custom Node",
    val countryCode: String = "CUSTOM",
    val flag: String = "🛡️",
    val configFileContent: String = "",
    val publicKey: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toVpnServer(): VpnServer {
        return VpnServer(
            id = id,
            name = serverName,
            location = "$location • $vpnProtocol",
            countryCode = countryCode,
            flag = flag,
            ip = hostOrIp,
            pingMs = 45,
            supportedPorts = listOf(port, 80, 443, 8080, 22, 1194, 51820),
            isCustom = true
        )
    }
}
