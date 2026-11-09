package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_validations")
data class ServerValidationResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: String,
    val serverName: String,
    val targetHost: String,
    val targetPort: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dnsResolved: Boolean,
    val resolvedIp: String?,
    val dnsError: String?,
    val pingMs: Long?,
    val tcpConnected: Boolean,
    val tcpError: String?,
    val tlsHandshakeSuccess: Boolean,
    val tlsCipher: String?,
    val tlsError: String?,
    val overallStatus: String // "ONLINE", "PORT_CLOSED", "DNS_FAILED", "TLS_FAILED", "UNREACHABLE"
)
