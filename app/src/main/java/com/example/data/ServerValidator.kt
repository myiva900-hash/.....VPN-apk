package com.example.data

import com.example.data.local.ServerValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

object ServerValidator {

    suspend fun validateServer(
        serverId: String,
        serverName: String,
        hostOrIp: String,
        port: Int,
        timeoutMs: Int = 4000,
        testTls: Boolean = true
    ): ServerValidationResult = withContext(Dispatchers.IO) {
        var dnsResolved = false
        var resolvedIp: String? = null
        var dnsError: String? = null
        var pingMs: Long? = null
        var tcpConnected = false
        var tcpError: String? = null
        var tlsHandshakeSuccess = false
        var tlsCipher: String? = null
        var tlsError: String? = null

        val startTime = System.currentTimeMillis()

        // 1. DNS Resolution Check
        var targetInetAddress: InetAddress? = null
        try {
            val addresses = InetAddress.getAllByName(hostOrIp)
            if (addresses.isNotEmpty()) {
                targetInetAddress = addresses.first()
                resolvedIp = targetInetAddress.hostAddress
                dnsResolved = true
            } else {
                dnsError = "No IP address returned by resolver"
            }
        } catch (e: UnknownHostException) {
            dnsError = "NXDOMAIN: Host '$hostOrIp' does not exist in public DNS"
        } catch (e: Exception) {
            dnsError = "DNS Error: ${e.message}"
        }

        if (!dnsResolved || targetInetAddress == null) {
            return@withContext ServerValidationResult(
                serverId = serverId,
                serverName = serverName,
                targetHost = hostOrIp,
                targetPort = port,
                dnsResolved = false,
                resolvedIp = null,
                dnsError = dnsError,
                pingMs = null,
                tcpConnected = false,
                tcpError = "Skipped (DNS failed)",
                tlsHandshakeSuccess = false,
                tlsCipher = null,
                tlsError = "Skipped (DNS failed)",
                overallStatus = "DNS_FAILED"
            )
        }

        // 2. TCP Port Check
        val tcpSocket = Socket()
        try {
            val connectStart = System.currentTimeMillis()
            tcpSocket.connect(InetSocketAddress(targetInetAddress, port), timeoutMs)
            val connectDuration = System.currentTimeMillis() - connectStart
            pingMs = connectDuration
            tcpConnected = true
        } catch (e: SocketTimeoutException) {
            tcpError = "Connection timed out after ${timeoutMs}ms. Port $port may be filtered by VPS firewall."
        } catch (e: Exception) {
            tcpError = "TCP Connect failed: ${e.message ?: e.javaClass.simpleName}"
        }

        if (!tcpConnected) {
            runCatching { tcpSocket.close() }
            return@withContext ServerValidationResult(
                serverId = serverId,
                serverName = serverName,
                targetHost = hostOrIp,
                targetPort = port,
                dnsResolved = true,
                resolvedIp = resolvedIp,
                dnsError = null,
                pingMs = null,
                tcpConnected = false,
                tcpError = tcpError,
                tlsHandshakeSuccess = false,
                tlsCipher = null,
                tlsError = "Skipped (TCP port closed)",
                overallStatus = "PORT_CLOSED"
            )
        }

        // 3. TLS Handshake Check (if port is 443 or TLS is requested)
        if (testTls && (port == 443 || port == 8443)) {
            var sslSocket: SSLSocket? = null
            try {
                val sslContext = SSLContext.getDefault()
                sslSocket = sslContext.socketFactory.createSocket(
                    tcpSocket,
                    targetInetAddress.hostAddress,
                    port,
                    true
                ) as SSLSocket

                val sslParams = sslSocket.sslParameters
                // Set legitimate SNI hostname
                if (!hostOrIp.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                    sslParams.serverNames = listOf(SNIHostName(hostOrIp))
                }
                sslSocket.sslParameters = sslParams
                sslSocket.soTimeout = timeoutMs

                sslSocket.startHandshake()
                val session = sslSocket.session
                tlsCipher = "${session.protocol} / ${session.cipherSuite}"
                tlsHandshakeSuccess = true
            } catch (e: Exception) {
                tlsError = "TLS Handshake failed: ${e.message}"
            } finally {
                runCatching { sslSocket?.close() }
            }
        } else {
            tlsHandshakeSuccess = true
            tlsCipher = "Plain TCP (Port $port)"
        }

        runCatching { tcpSocket.close() }

        val overallStatus = when {
            !dnsResolved -> "DNS_FAILED"
            !tcpConnected -> "PORT_CLOSED"
            testTls && (port == 443 || port == 8443) && !tlsHandshakeSuccess -> "TLS_FAILED"
            else -> "ONLINE"
        }

        ServerValidationResult(
            serverId = serverId,
            serverName = serverName,
            targetHost = hostOrIp,
            targetPort = port,
            dnsResolved = dnsResolved,
            resolvedIp = resolvedIp,
            dnsError = dnsError,
            pingMs = pingMs,
            tcpConnected = tcpConnected,
            tcpError = tcpError,
            tlsHandshakeSuccess = tlsHandshakeSuccess,
            tlsCipher = tlsCipher,
            tlsError = tlsError,
            overallStatus = overallStatus
        )
    }
}
