package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerValidator
import com.example.data.local.ServerValidationResult
import com.example.model.VpnServer
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnConnectedGreen
import com.example.ui.theme.VpnConnectingYellow
import com.example.ui.theme.VpnErrorRed
import com.example.ui.theme.VpnRedPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerValidationScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val servers by viewModel.servers.collectAsState()
    val validationHistory by viewModel.validationHistory.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedServer by remember {
        mutableStateOf(servers.find { it.id == "de_fra" } ?: servers.first())
    }
    var targetHost by remember { mutableStateOf(selectedServer.ip) }
    var targetPortText by remember { mutableStateOf("443") }
    var isValidating by remember { mutableStateOf(false) }
    var latestResult by remember { mutableStateOf<ServerValidationResult?>(null) }
    var showServerDropdown by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Test, 1: VPS Setup Guide, 2: History

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Server Diagnostics & Validation",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("validation_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VpnRedPrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = VpnRedPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Validation Test", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("VPS Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Room History (${validationHistory.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Live Validation Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Target VPN Server Inspection",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VpnRedPrimary
                        )
                        Text(
                            text = "Perform authentic DNS resolution, TCP socket reachability, and TLS certificate handshake verification without simulation.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Selector Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "SELECT SERVER PRESET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showServerDropdown = true }
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedServer.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedServer.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${selectedServer.location} • ${selectedServer.ip}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showServerDropdown,
                                        onDismissRequest = { showServerDropdown = false }
                                    ) {
                                        servers.forEach { server ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(server.flag, fontSize = 16.sp)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(server.name, fontWeight = FontWeight.Bold)
                                                            Text(server.ip, fontSize = 11.sp, color = Color.Gray)
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    selectedServer = server
                                                    targetHost = server.ip
                                                    showServerDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = targetHost,
                                    onValueChange = { targetHost = it },
                                    label = { Text("Server Hostname or IP") },
                                    placeholder = { Text("e.g. de-fra.vpn.mutigandayvan.net") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = targetPortText,
                                    onValueChange = { targetPortText = it },
                                    label = { Text("Target Port") },
                                    placeholder = { Text("443") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isValidating = true
                                scope.launch {
                                    val port = targetPortText.toIntOrNull() ?: 443
                                    val result = ServerValidator.validateServer(
                                        serverId = selectedServer.id,
                                        serverName = selectedServer.name,
                                        hostOrIp = targetHost.trim(),
                                        port = port,
                                        timeoutMs = 4500,
                                        testTls = (port == 443 || port == 8443)
                                    )
                                    latestResult = result
                                    viewModel.saveValidationResult(result)
                                    isValidating = false
                                }
                            },
                            enabled = !isValidating && targetHost.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_validation_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
                        ) {
                            if (isValidating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Validating Network Stack...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Authentic Server Diagnostics", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Results Display
                        latestResult?.let { res ->
                            ValidationResultCard(res)
                        }
                    }
                }

                1 -> {
                    // VPS Setup Guide Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Legitimate VPS Setup Requirements",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VpnRedPrimary
                        )
                        Text(
                            text = "If testing de-fra.vpn.mutigandayvan.net or a private VPS, the following 5 components must be configured on the server for the VPN socket to connect without closing:",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        VpsConfigStepCard(
                            step = "1. Public DNS A-Record",
                            desc = "The hostname 'de-fra.vpn.mutigandayvan.net' currently returns NXDOMAIN (Non-Existent Domain). You must register the domain and add an A-Record in your DNS registrar pointing to your VPS public IP address."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VpsConfigStepCard(
                            step = "2. Firewall Inbound Ports (UFW/Iptables)",
                            desc = "Ensure the VPS firewall allows TCP traffic on the configured port:\nsudo ufw allow 443/tcp\nsudo ufw allow 80/tcp\nsudo ufw allow 53/udp"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VpsConfigStepCard(
                            step = "3. Tunnel Daemon (OpenVPN / Xray / Shadowsocks)",
                            desc = "A legitimate VPN server daemon must be listening on the specified port. If no daemon is bound to port 443, incoming TCP SYN packets receive immediate RST (Connection Refused)."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VpsConfigStepCard(
                            step = "4. Valid TLS Certificate (Let's Encrypt)",
                            desc = "For TLS/SNI mode, obtain a legitimate TLS certificate for de-fra.vpn.mutigandayvan.net so Android's TrustManager can authenticate the server without throwing SSLHandshakeException."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VpsConfigStepCard(
                            step = "5. Kernel IP Forwarding & NAT Masquerade",
                            desc = "Enable routing on the Linux VPS so client packets can access the internet:\nsysctl -w net.ipv4.ip_forward=1\niptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE"
                        )
                    }
                }

                2 -> {
                    // Room Database Validation History Tab
                    if (validationHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No server validation runs stored in Room database yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(validationHistory) { item ->
                                ValidationHistoryItemRow(item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationResultCard(res: ServerValidationResult) {
    val overallColor = when (res.overallStatus) {
        "ONLINE" -> VpnConnectedGreen
        "PORT_CLOSED", "TLS_FAILED" -> VpnConnectingYellow
        else -> VpnErrorRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALIDATION SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnRedPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(overallColor.copy(alpha = 0.15f))
                        .border(1.dp, overallColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = res.overallStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = overallColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: DNS Resolution
            DiagnosticRow(
                title = "1. DNS Resolution",
                success = res.dnsResolved,
                detail = if (res.dnsResolved) "Resolved -> ${res.resolvedIp}" else res.dnsError ?: "DNS Failed"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step 2: TCP Port Connection
            DiagnosticRow(
                title = "2. TCP Socket Port (${res.targetPort})",
                success = res.tcpConnected,
                detail = if (res.tcpConnected) "Connected in ${res.pingMs}ms (Socket protected)" else res.tcpError ?: "Port unreachable"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Step 3: TLS Handshake & SNI
            DiagnosticRow(
                title = "3. TLS / SNI Handshake",
                success = res.tlsHandshakeSuccess,
                detail = if (res.tlsHandshakeSuccess) "Handshake OK: ${res.tlsCipher}" else res.tlsError ?: "TLS Check Failed"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (res.overallStatus != "ONLINE") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VpnErrorRed.copy(alpha = 0.08f))
                        .border(1.dp, VpnErrorRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = VpnErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (res.overallStatus) {
                                "DNS_FAILED" -> "Host '${res.targetHost}' does not exist in DNS. You cannot connect until a public DNS A-record points to the server IP."
                                "PORT_CLOSED" -> "Server IP was resolved, but port ${res.targetPort} refused or timed out. Check VPS firewall and tunnel daemon."
                                "TLS_FAILED" -> "TCP connected, but TLS handshake failed. Check certificate validity or domain SNI configuration."
                                else -> "Server connection verification failed."
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    success: Boolean,
    detail: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (success) VpnConnectedGreen else VpnErrorRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (success) MaterialTheme.colorScheme.onSurfaceVariant else VpnErrorRed
            )
        }
    }
}

@Composable
private fun VpsConfigStepCard(step: String, desc: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = step,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = VpnRedPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ValidationHistoryItemRow(res: ServerValidationResult) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(res.timestamp))
    val statusColor = when (res.overallStatus) {
        "ONLINE" -> VpnConnectedGreen
        "PORT_CLOSED", "TLS_FAILED" -> VpnConnectingYellow
        else -> VpnErrorRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${res.targetHost}:${res.targetPort}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$dateStr • ${res.serverName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = res.overallStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
