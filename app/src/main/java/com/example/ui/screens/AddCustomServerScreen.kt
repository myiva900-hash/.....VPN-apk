package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerValidator
import com.example.data.local.ServerValidationResult
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnConnectedGreen
import com.example.ui.theme.VpnErrorRed
import com.example.ui.theme.VpnRedPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomServerScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editingProfile by viewModel.editingProfile.collectAsState()

    var serverName by remember { mutableStateOf("") }
    var hostOrIp by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("443") }
    var username by remember { mutableStateOf("") }
    var passwordPlain by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedProtocol by remember { mutableStateOf("OpenVPN") }
    var location by remember { mutableStateOf("Kigali, Rwanda") }
    var countryCode by remember { mutableStateOf("RW") }
    var flagEmoji by remember { mutableStateOf("🇷🇼") }
    var configFileContent by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }

    var protocolDropdownExpanded by remember { mutableStateOf(false) }
    val supportedProtocols = listOf("OpenVPN", "WireGuard", "SSH", "Direct TLS", "Shadowsocks")

    // Validation & Diagnostic state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTestingReachability by remember { mutableStateOf(false) }
    var reachabilityResult by remember { mutableStateOf<ServerValidationResult?>(null) }

    // Populate when editing existing profile
    LaunchedEffect(editingProfile) {
        editingProfile?.let { p ->
            serverName = p.serverName
            hostOrIp = p.hostOrIp
            portText = p.port.toString()
            username = p.username
            passwordPlain = "" // Keep blank so user can either leave untouched or type new
            selectedProtocol = p.vpnProtocol
            location = p.location
            countryCode = p.countryCode
            flagEmoji = p.flag
            configFileContent = p.configFileContent
            publicKey = p.publicKey
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingProfile != null) "Edit Server Profile" else "Add Custom Server",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("add_server_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 0
                            viewModel.saveCustomServerProfile(
                                id = editingProfile?.id,
                                serverName = serverName,
                                hostOrIp = hostOrIp,
                                port = port,
                                username = username,
                                passwordPlain = passwordPlain,
                                vpnProtocol = selectedProtocol,
                                location = location,
                                countryCode = countryCode,
                                flag = flagEmoji,
                                configFileContent = configFileContent,
                                publicKey = publicKey
                            ) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_server_top_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Server Profile",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info & Security Notice
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = VpnRedPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = VpnRedPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mutiganda Yvan VPN Server Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = VpnRedPrimary
                        )
                        Text(
                            text = "Hardware-backed Android Keystore AES-256 GCM encryption. Credentials are saved safely and never printed to logs.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error banner if any
            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = VpnErrorRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = VpnErrorRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = VpnErrorRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // SECTION 1: SERVER IDENTIFICATION
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Server Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Server Name
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = {
                            serverName = it
                            errorMessage = null
                        },
                        label = { Text("Server Name *") },
                        placeholder = { Text("e.g. Frankfurt Fast Node or Kigali VIP") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // IP / Hostname
                    OutlinedTextField(
                        value = hostOrIp,
                        onValueChange = {
                            hostOrIp = it
                            errorMessage = null
                        },
                        label = { Text("IP Address or Hostname *") },
                        placeholder = { Text("e.g. 198.51.100.22 or vpn.mutigandayvan.net") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_host_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Port
                    OutlinedTextField(
                        value = portText,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) portText = it
                            errorMessage = null
                        },
                        label = { Text("Port (1 - 65535) *") },
                        placeholder = { Text("443") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_port_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Port Selection Chips
                    Text(
                        text = "Common Ports:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(443, 80, 22, 1194, 51820, 8080).forEach { p ->
                            SuggestionChip(
                                onClick = { portText = p.toString() },
                                label = { Text(":$p", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (portText == p.toString()) VpnRedPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Location City & Country
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Server Location") },
                            placeholder = { Text("e.g. Kigali, Rwanda") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("server_location_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = flagEmoji,
                            onValueChange = { flagEmoji = it },
                            label = { Text("Flag") },
                            singleLine = true,
                            modifier = Modifier
                                .width(80.dp)
                                .testTag("server_flag_input")
                        )
                    }
                }
            }

            // SECTION 2: PROTOCOL SELECTION
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. VPN Protocol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = protocolDropdownExpanded,
                        onExpandedChange = { protocolDropdownExpanded = !protocolDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProtocol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tunnel Protocol") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("server_protocol_selector")
                        )
                        ExposedDropdownMenu(
                            expanded = protocolDropdownExpanded,
                            onDismissRequest = { protocolDropdownExpanded = false }
                        ) {
                            supportedProtocols.forEach { proto ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(proto, fontWeight = FontWeight.SemiBold)
                                            val desc = when (proto) {
                                                "OpenVPN" -> "SSL/TLS encrypted virtual TAP/TUN tunnel"
                                                "WireGuard" -> "High-speed modern crypto tunnel (ChaCha20)"
                                                "SSH" -> "SSH-2.0 dynamic port forwarding gateway"
                                                "Direct TLS" -> "Native TLS socket transport with custom SNI"
                                                "Shadowsocks" -> "Encrypted proxy protocol"
                                                else -> ""
                                            }
                                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        selectedProtocol = proto
                                        protocolDropdownExpanded = false
                                        // Auto-adjust default port if user hasn't explicitly customized
                                        when (proto) {
                                            "SSH" -> if (portText == "443" || portText == "1194") portText = "22"
                                            "OpenVPN" -> if (portText == "22" || portText == "51820") portText = "1194"
                                            "WireGuard" -> if (portText == "22" || portText == "1194") portText = "51820"
                                            "Direct TLS" -> if (portText == "22" || portText == "1194") portText = "443"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: AUTHENTICATION & CREDENTIALS
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Authentication Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Account ID") },
                        placeholder = { Text("e.g. vpnuser or root") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_username_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password (Masked with toggle)
                    OutlinedTextField(
                        value = passwordPlain,
                        onValueChange = { passwordPlain = it },
                        label = {
                            Text(
                                if (editingProfile != null && passwordPlain.isEmpty()) "Password (saved encrypted - type to change)"
                                else "Password / Auth Token"
                            )
                        },
                        placeholder = { Text("Enter password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                modifier = Modifier.testTag("password_visibility_toggle")
                            ) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_password_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = VpnConnectedGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stored encrypted via Android Keystore. Passwords are never revealed in logs.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SECTION 4: PROTOCOL-SPECIFIC CONFIGURATION
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Protocol Advanced Config (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedProtocol == "WireGuard") {
                        OutlinedTextField(
                            value = publicKey,
                            onValueChange = { publicKey = it },
                            label = { Text("Server Public Key (base64)") },
                            placeholder = { Text("e.g. aB12cD34...=") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_wireguard_key_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = configFileContent,
                        onValueChange = { configFileContent = it },
                        label = { Text("Custom Config Script / Profile (.ovpn / payload)") },
                        placeholder = { Text("Paste raw configuration directives or headers here...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_config_input")
                    )
                }
            }

            // SECTION 5: REACHABILITY DIAGNOSTIC TEST (Safe: Does NOT claim connected)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5. Reachability Diagnostic",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tests DNS resolution and TCP handshake without modifying VPN state. A successful test verifies network path only; VPN connection requires user START.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val host = hostOrIp.trim()
                            val port = portText.toIntOrNull() ?: 443
                            if (host.isBlank()) {
                                errorMessage = "Enter Hostname or IP to run reachability check."
                                return@OutlinedButton
                            }
                            isTestingReachability = true
                            errorMessage = null
                            scope.launch {
                                val result = ServerValidator.validateServer(
                                    serverId = editingProfile?.id ?: "temp_check",
                                    serverName = serverName.ifBlank { "Custom Server" },
                                    hostOrIp = host,
                                    port = port,
                                    testTls = selectedProtocol == "Direct TLS" || port == 443
                                )
                                reachabilityResult = result
                                isTestingReachability = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_reachability_button"),
                        enabled = !isTestingReachability
                    ) {
                        if (isTestingReachability) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Reachability...")
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Server Reachability")
                        }
                    }

                    reachabilityResult?.let { res ->
                        val isReachable = res.tcpConnected
                        val err = res.tcpError ?: res.dnsError ?: "Connection refused"
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isReachable) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isReachable) VpnConnectedGreen else VpnErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isReachable) "Reachable: ${res.resolvedIp ?: hostOrIp} (${res.pingMs ?: 0} ms)" else "Unreachable: $err",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isReachable) VpnConnectedGreen else VpnErrorRed
                                )
                                Text(
                                    text = "TLS Status: ${if (res.tlsHandshakeSuccess) "TLS Handshake OK (${res.tlsCipher ?: "Active"})" else "TLS Not established / Plain socket"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 6: SAVE BUTTON
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 0
                    viewModel.saveCustomServerProfile(
                        id = editingProfile?.id,
                        serverName = serverName,
                        hostOrIp = hostOrIp,
                        port = port,
                        username = username,
                        passwordPlain = passwordPlain,
                        vpnProtocol = selectedProtocol,
                        location = location,
                        countryCode = countryCode,
                        flag = flagEmoji,
                        configFileContent = configFileContent,
                        publicKey = publicKey
                    ) { success, msg ->
                        if (success) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_server_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingProfile != null) "Update Server Profile" else "Save Server Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
