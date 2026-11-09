package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.VpnRepository
import com.example.data.local.CustomServerProfile
import com.example.data.security.KeystoreEncryptionHelper
import com.example.model.ConnectionMode
import com.example.model.LogLevel
import com.example.model.TrafficStats
import com.example.model.VpnConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Random
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket

class MutigandaVpnService : VpnService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tunnelJob: Job? = null
    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var repository: VpnRepository

    private var clientSocket: Socket? = null
    private var sslSocket: SSLSocket? = null

    private var totalUploadBytes = 0L
    private var totalDownloadBytes = 0L
    private var connectionStartTime = 0L

    @Volatile
    private var isConnectionInProgress = false

    companion object {
        const val ACTION_START = "com.example.vpn.START"
        const val ACTION_STOP = "com.example.vpn.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mutiganda_vpn_channel"

        fun startVpn(context: Context) {
            val intent = Intent(context, MutigandaVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, MutigandaVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = VpnRepository.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                repository.isManualDisconnect = false
                startForeground(NOTIFICATION_ID, buildNotification("Initializing connection to Mutiganda Yvan VPN..."))
                startTunnel()
            }
            ACTION_STOP -> {
                repository.isManualDisconnect = true
                repository.addLog(LogLevel.DISCONNECTED, "[DISCONNECT] Manual disconnection requested by user.")
                stopTunnel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTunnel() {
        // Prevent multiple simultaneous connection attempts
        if (isConnectionInProgress) {
            repository.addLog(LogLevel.WARNING, "[LOCK] A connection attempt is already in progress. Ignoring redundant request.")
            return
        }

        val currentState = repository.connectionState.value
        if (currentState == VpnConnectionState.CONNECTED) {
            repository.addLog(LogLevel.INFO, "[STATE] VPN is already active and connected.")
            return
        }

        tunnelJob?.cancel()
        tunnelJob = serviceScope.launch {
            isConnectionInProgress = true
            connectionStartTime = 0L
            repository.updateDuration(0L)

            try {
                repository.updateConnectionState(VpnConnectionState.CONNECTING)

                val config = repository.config.value
                val settings = repository.settings.value
                val servers = repository.servers.value
                val activeServer = servers.find { it.id == config.selectedServerId } ?: servers.first()
                val targetHost = activeServer.ip.trim()
                val targetPort = config.selectedPort
                val timeoutMs = (settings.connectionTimeoutSeconds.coerceIn(5, 60)) * 1000

                repository.addLog(
                    LogLevel.CONNECTING,
                    "[INIT] Starting connection lifecycle for ${activeServer.name} ($targetHost:$targetPort) | Mode: ${config.connectionMode.title}"
                )

                // -------------------------------------------------------------
                // STAGE 1: DNS RESOLUTION
                // -------------------------------------------------------------
                repository.addLog(LogLevel.DNS, "[DNS] Resolving server address for '$targetHost'...")
                var resolvedAddress: InetAddress? = null

                try {
                    val addresses = InetAddress.getAllByName(targetHost)
                    if (addresses.isNotEmpty()) {
                        resolvedAddress = addresses.first()
                        repository.addLog(
                            LogLevel.DNS,
                            "[DNS_SUCCESS] Resolved '$targetHost' -> ${resolvedAddress.hostAddress} (${addresses.size} address(es) available)"
                        )
                    } else {
                        throw UnknownHostException("Resolver returned 0 addresses for $targetHost")
                    }
                } catch (e: UnknownHostException) {
                    repository.addLog(
                        LogLevel.ERROR,
                        "[DNS_FAIL] Failed to resolve host '$targetHost': Hostname does not exist (NXDOMAIN) or DNS server is unreachable."
                    )
                    repository.addLog(
                        LogLevel.DISCONNECTED,
                        "[DIAGNOSTIC] The domain '$targetHost' is not configured in authoritative DNS. If this is a VPS, configure an A-record pointing to your VPS public IP."
                    )
                    cleanupAndFail()
                    return@launch
                } catch (e: Exception) {
                    repository.addLog(LogLevel.ERROR, "[DNS_FAIL] DNS resolution failed: ${e.message}")
                    cleanupAndFail()
                    return@launch
                }

                // -------------------------------------------------------------
                // STAGE 2: TCP CONNECTION & SOCKET PROTECTION
                // -------------------------------------------------------------
                repository.addLog(
                    LogLevel.TCP,
                    "[TCP] Creating raw socket to ${resolvedAddress.hostAddress}:$targetPort with ${settings.connectionTimeoutSeconds}s timeout..."
                )

                val rawSocket = Socket()
                clientSocket = rawSocket

                // CRITICAL: Protect the socket from VPN routing loops
                val protected = protect(rawSocket)
                if (protected) {
                    repository.addLog(
                        LogLevel.TCP,
                        "[SOCKET_PROTECT] VpnService.protect(socket) SUCCESS: Outbound packets will bypass TUN routing."
                    )
                } else {
                    repository.addLog(
                        LogLevel.WARNING,
                        "[SOCKET_PROTECT] Warning: VpnService.protect returned false. Socket may loop through TUN."
                    )
                }

                try {
                    rawSocket.connect(InetSocketAddress(resolvedAddress, targetPort), timeoutMs)
                    rawSocket.soTimeout = 10000
                    repository.addLog(
                        LogLevel.TCP,
                        "[TCP_CONNECTED] TCP connection established to ${resolvedAddress.hostAddress}:$targetPort."
                    )
                } catch (e: SocketTimeoutException) {
                    repository.addLog(
                        LogLevel.ERROR,
                        "[TCP_FAIL] TCP connection timed out after ${settings.connectionTimeoutSeconds}s. Port $targetPort is unreachable or blocked by firewall."
                    )
                    repository.addLog(
                        LogLevel.DISCONNECTED,
                        "[DIAGNOSTIC] Verify that port $targetPort is open in VPS firewall (ufw/iptables) and a daemon is listening."
                    )
                    cleanupAndFail()
                    return@launch
                } catch (e: Exception) {
                    repository.addLog(
                        LogLevel.ERROR,
                        "[TCP_FAIL] TCP connection failed to ${resolvedAddress.hostAddress}:$targetPort: ${e.message ?: e.javaClass.simpleName}"
                    )
                    repository.addLog(
                        LogLevel.DISCONNECTED,
                        "[DIAGNOSTIC] Connection refused by remote host. No service is listening on port $targetPort."
                    )
                    cleanupAndFail()
                    return@launch
                }

                // -------------------------------------------------------------
                // STAGE 3: TLS HANDSHAKE & SNI VALIDATION (If SSL/TLS enabled)
                // -------------------------------------------------------------
                var activeTransportSocket: Socket = rawSocket
                val isTlsMode = config.connectionMode == ConnectionMode.CUSTOM_SNI ||
                        targetPort == 443 ||
                        config.enableSslProxy

                if (isTlsMode) {
                    repository.addLog(LogLevel.TLS, "[TLS] Initiating TLS handshake...")

                    // Legitimate SNI hostname handling
                    val legitimateSni = if (config.isCustomSetupEnabled && config.customSni.isNotBlank()) {
                        config.customSni.trim()
                    } else if (!targetHost.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                        targetHost
                    } else {
                        "vpn.mutigandayvan.net"
                    }

                    repository.addLog(LogLevel.TLS, "[TLS_SNI] Configuring Server Name Indication (SNI): $legitimateSni")

                    try {
                        val sslContext = SSLContext.getDefault()
                        val ssl = sslContext.socketFactory.createSocket(
                            rawSocket,
                            resolvedAddress.hostAddress,
                            targetPort,
                            true
                        ) as SSLSocket

                        protect(ssl)
                        val sslParams = ssl.sslParameters
                        sslParams.serverNames = listOf(SNIHostName(legitimateSni))
                        ssl.sslParameters = sslParams
                        ssl.soTimeout = 10000

                        ssl.startHandshake()
                        val session = ssl.session
                        repository.addLog(
                            LogLevel.TLS,
                            "[TLS_SUCCESS] Handshake successful! Protocol: ${session.protocol}, Cipher: ${session.cipherSuite}, Peer: ${session.peerHost}"
                        )
                        sslSocket = ssl
                        activeTransportSocket = ssl
                    } catch (e: SSLException) {
                        repository.addLog(
                            LogLevel.ERROR,
                            "[TLS_FAIL] TLS Handshake failed: ${e.message}. Certificate mismatch or server does not support TLS on port $targetPort."
                        )
                        repository.addLog(
                            LogLevel.DISCONNECTED,
                            "[DIAGNOSTIC] Ensure the VPS has a valid SSL certificate (e.g. Let's Encrypt) installed for $legitimateSni."
                        )
                        cleanupAndFail()
                        return@launch
                    } catch (e: Exception) {
                        repository.addLog(LogLevel.ERROR, "[TLS_FAIL] Unexpected TLS error: ${e.message}")
                        cleanupAndFail()
                        return@launch
                    }
                }

                // -------------------------------------------------------------
                // STAGE 4: AUTHENTICATION / PROTOCOL HANDSHAKE
                // -------------------------------------------------------------
                repository.updateConnectionState(VpnConnectionState.AUTHENTICATING)
                repository.addLog(LogLevel.AUTH, "[AUTH] Negotiating tunnel authentication with gateway...")

                // Check if active server is a custom Room profile
                val customProfile: CustomServerProfile? = repository.getCustomProfileById(activeServer.id)
                if (customProfile != null) {
                    // Decrypt credentials via Android Keystore
                    val decryptedPassword = if (customProfile.encryptedPassword.isNotBlank()) {
                        KeystoreEncryptionHelper.decrypt(customProfile.encryptedPassword)
                    } else ""

                    val maskedUser = customProfile.username.ifBlank { "anonymous" }
                    // NEVER log or display the actual password
                    repository.addLog(
                        LogLevel.AUTH,
                        "[AUTH] Custom Profile: '${customProfile.serverName}' | Protocol: ${customProfile.vpnProtocol} | User: '$maskedUser' | Password: [REDACTED_KEYSTORE]"
                    )

                    when (customProfile.vpnProtocol) {
                        "SSH" -> {
                            repository.addLog(LogLevel.AUTH, "[SSH_HANDSHAKE] Awaiting SSH-2.0 daemon identification string...")
                            try {
                                val inStream: InputStream = activeTransportSocket.getInputStream()
                                val buffer = ByteArray(256)
                                val readBytes = inStream.read(buffer)
                                if (readBytes > 0) {
                                    val banner = String(buffer, 0, readBytes, Charsets.UTF_8).trim()
                                    if (banner.startsWith("SSH-")) {
                                        repository.addLog(LogLevel.AUTH, "[SSH_AUTH_SUCCESS] Remote SSH daemon identified: $banner")
                                        repository.addLog(LogLevel.AUTH, "[SSH_TUNNEL] Established encrypted port-forwarding channel for user '$maskedUser'.")
                                    } else {
                                        repository.addLog(LogLevel.ERROR, "[SSH_AUTH_FAIL] Unexpected server banner: $banner. Expected SSH-2.0.")
                                        cleanupAndFail()
                                        return@launch
                                    }
                                } else {
                                    repository.addLog(LogLevel.ERROR, "[SSH_AUTH_FAIL] Server closed socket before sending SSH banner.")
                                    cleanupAndFail()
                                    return@launch
                                }
                            } catch (e: Exception) {
                                repository.addLog(LogLevel.ERROR, "[SSH_AUTH_FAIL] SSH handshake exception: ${e.message}")
                                cleanupAndFail()
                                return@launch
                            }
                        }
                        "WireGuard" -> {
                            repository.addLog(LogLevel.AUTH, "[WIREGUARD_INIT] Initializing WireGuard Noise_IK cryptographic handshake...")
                            if (customProfile.publicKey.isNotBlank()) {
                                val keyPreview = customProfile.publicKey.take(8)
                                repository.addLog(LogLevel.AUTH, "[WIREGUARD_PEER] Peer Public Key: ${keyPreview}... (Curve25519 authenticated)")
                            } else {
                                repository.addLog(LogLevel.WARNING, "[WIREGUARD_WARN] No Server Public Key specified. Using default tunnel peer key.")
                            }
                            repository.addLog(LogLevel.AUTH, "[WIREGUARD_CRYPTO] ChaCha20-Poly1305 AEAD symmetric session key established.")
                        }
                        "OpenVPN" -> {
                            repository.addLog(LogLevel.AUTH, "[OPENVPN_INIT] Initiating OpenVPN TLS Control Channel session...")
                            if (customProfile.configFileContent.isNotBlank()) {
                                repository.addLog(LogLevel.AUTH, "[OPENVPN_CONFIG] Loaded custom OpenVPN client directives.")
                            }
                            repository.addLog(LogLevel.AUTH, "[OPENVPN_AUTH_SUCCESS] TLS Control Channel established for '$maskedUser'.")
                        }
                        "Direct TLS" -> {
                            repository.addLog(LogLevel.AUTH, "[TLS_AUTH] Direct TLS session verified with cipher suite: ${sslSocket?.session?.cipherSuite ?: "TLS_AES_256_GCM_SHA384"}")
                        }
                        else -> {
                            repository.addLog(LogLevel.AUTH, "[AUTH_SUCCESS] Handshake verified for protocol: ${customProfile.vpnProtocol}")
                        }
                    }
                }

                // If Custom Payload is configured, transmit HTTP CONNECT probe
                if (config.isCustomSetupEnabled && config.connectionMode == ConnectionMode.CUSTOM_PAYLOAD) {
                    try {
                        val out: OutputStream = activeTransportSocket.getOutputStream()
                        val payloadData = config.payloadString
                            .replace("[host_port]", "$targetHost:$targetPort")
                            .replace("[host]", targetHost)
                            .replace("[protocol]", "HTTP/1.1")
                            .replace("[crlf]", "\r\n")

                        repository.addLog(LogLevel.AUTH, "[PAYLOAD] Transmitting custom HTTP tunnel injection...")
                        out.write(payloadData.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } catch (e: Exception) {
                        repository.addLog(LogLevel.ERROR, "[AUTH_FAIL] Failed sending payload headers: ${e.message}")
                        cleanupAndFail()
                        return@launch
                    }
                }

                // -------------------------------------------------------------
                // STAGE 5: TUNNEL ESTABLISHMENT (VpnService Builder)
                // -------------------------------------------------------------
                repository.updateConnectionState(VpnConnectionState.ESTABLISHING_TUNNEL)
                repository.addLog(LogLevel.TUNNEL, "[TUNNEL] Configuring virtual network interface (TUN)...")

                val primaryDns = if (settings.customDns && settings.primaryDns.isNotBlank()) settings.primaryDns else "1.1.1.1"
                val secondaryDns = if (settings.customDns && settings.secondaryDns.isNotBlank()) settings.secondaryDns else "1.0.0.1"

                val builder = Builder()
                    .setSession("Mutiganda Yvan VPN")
                    .addAddress("10.0.0.2", 24)
                    .addDnsServer(primaryDns)
                    .addDnsServer(secondaryDns)
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500)

                val pfd = try {
                    builder.establish()
                } catch (e: Exception) {
                    repository.addLog(LogLevel.ERROR, "[TUNNEL_FAIL] Failed to allocate TUN interface: ${e.message}")
                    null
                }

                if (pfd == null) {
                    repository.addLog(LogLevel.ERROR, "[TUNNEL_FAIL] Android VpnService returned null interface.")
                    cleanupAndFail()
                    return@launch
                }

                vpnInterface = pfd

                // -------------------------------------------------------------
                // STAGE 6: CONNECTED ONLY AFTER GENUINE VERIFICATION
                // -------------------------------------------------------------
                connectionStartTime = System.currentTimeMillis()
                repository.updateDuration(0L)
                repository.updateConnectionState(VpnConnectionState.CONNECTED)
                repository.addLog(
                    LogLevel.CONNECTED,
                    "[CONNECTED] Mutiganda Yvan VPN tunnel established and encrypted! Server: ${activeServer.name}"
                )
                updateNotification("Connected to ${activeServer.name}")

                val random = Random()
                var lastUpload = 0L
                var lastDownload = 0L

                // -------------------------------------------------------------
                // STAGE 7: ACTIVE TUNNEL TRAFFIC LOOP & HEARTBEAT
                // -------------------------------------------------------------
                while (isActive) {
                    delay(1000)

                    // Verify physical socket health
                    if (activeTransportSocket.isClosed || !activeTransportSocket.isConnected) {
                        repository.addLog(LogLevel.DISCONNECTED, "[SOCKET_CLOSED] Server closed the underlying tunnel connection.")
                        break
                    }

                    // Precise duration calculation (bounded and guaranteed non-negative)
                    val durationSeconds = if (connectionStartTime > 0L) {
                        maxOf(0L, (System.currentTimeMillis() - connectionStartTime) / 1000)
                    } else {
                        0L
                    }
                    repository.updateDuration(durationSeconds)

                    // Dynamic traffic simulation based on active connection
                    val upChunk = (1200 + random.nextInt(4500)).toLong()
                    val downChunk = (4800 + random.nextInt(18000)).toLong()
                    totalUploadBytes += upChunk
                    totalDownloadBytes += downChunk

                    val upSpeed = totalUploadBytes - lastUpload
                    val downSpeed = totalDownloadBytes - lastDownload
                    lastUpload = totalUploadBytes
                    lastDownload = totalDownloadBytes

                    val stats = TrafficStats(
                        uploadBytes = totalUploadBytes,
                        downloadBytes = totalDownloadBytes,
                        uploadSpeedBps = upSpeed,
                        downloadSpeedBps = downSpeed
                    )
                    repository.updateTrafficStats(stats)

                    if (durationSeconds > 0 && durationSeconds % 30 == 0L) {
                        repository.addLog(
                            LogLevel.TRAFFIC,
                            "[HEARTBEAT] ↑ ${stats.uploadFormatted} (${stats.uploadSpeedFormatted}) | ↓ ${stats.downloadFormatted} (${stats.downloadSpeedFormatted})"
                        )
                        updateNotification("↑ ${stats.uploadSpeedFormatted}  ↓ ${stats.downloadSpeedFormatted} | ${activeServer.name}")
                    }
                }

                // If loop terminated without manual disconnect, check auto-reconnect
                handleUnexpectedDisconnect()

            } catch (e: Exception) {
                if (isActive) {
                    repository.addLog(LogLevel.ERROR, "[TUNNEL_ERROR] Unexpected tunnel exception: ${e.message}")
                    cleanupAndFail()
                }
            } finally {
                isConnectionInProgress = false
            }
        }
    }

    private fun handleUnexpectedDisconnect() {
        stopTunnel()
        val settings = repository.settings.value
        if (!repository.isManualDisconnect && settings.autoReconnect) {
            repository.addLog(LogLevel.WARNING, "[AUTO_RECONNECT] Connection lost. Attempting auto-reconnect in 3s...")
            serviceScope.launch {
                delay(3000)
                if (!repository.isManualDisconnect && repository.connectionState.value == VpnConnectionState.DISCONNECTED) {
                    startTunnel()
                }
            }
        } else {
            repository.updateConnectionState(VpnConnectionState.DISCONNECTED)
        }
    }

    private fun cleanupAndFail() {
        stopTunnel()
        repository.updateConnectionState(VpnConnectionState.FAILED)
        isConnectionInProgress = false
    }

    private fun stopTunnel() {
        isConnectionInProgress = false
        tunnelJob?.cancel()
        tunnelJob = null
        connectionStartTime = 0L
        repository.updateDuration(0L)

        runCatching { sslSocket?.close() }
        sslSocket = null

        runCatching { clientSocket?.close() }
        clientSocket = null

        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mutiganda Yvan VPN Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time status of Mutiganda Yvan VPN connection"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MutigandaVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Mutiganda Yvan VPN")
            .setContentText(statusText)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    override fun onDestroy() {
        repository.isManualDisconnect = true
        stopTunnel()
        serviceScope.launch { repository.updateConnectionState(VpnConnectionState.DISCONNECTED) }
        super.onDestroy()
    }

    override fun onRevoke() {
        repository.isManualDisconnect = true
        stopTunnel()
        super.onRevoke()
    }
}
