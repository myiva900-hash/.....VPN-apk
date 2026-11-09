package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionMode
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnRedPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DnsTunnelScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.config.collectAsState()
    val context = LocalContext.current

    var nsDomain by remember { mutableStateOf(config.dnsNameserver) }
    var resolverIp by remember { mutableStateOf(config.dnsResolver) }
    var pubKey by remember { mutableStateOf(config.dnsPublicKey) }
    var queryType by remember { mutableStateOf("UDP (Port 53)") }

    val dnsPresets = listOf(
        "Cloudflare (1.1.1.1)" to "1.1.1.1",
        "Google (8.8.8.8)" to "8.8.8.8",
        "Quad9 (9.9.9.9)" to "9.9.9.9",
        "AdGuard (94.140.14.14)" to "94.140.14.14"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DNS Tunnel (DNSTT)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dns_tunnel_back_button")) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = VpnRedPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DNSTT Tunnel Setup",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnRedPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tunnels full IP traffic through recursive DNS query packets. Effective in restricted firewalls where standard VPN ports are filtered.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = nsDomain,
                onValueChange = { nsDomain = it },
                label = { Text("Nameserver (NS) Domain") },
                placeholder = { Text("e.g. t.mutigandavpn.net") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dns_ns_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = resolverIp,
                onValueChange = { resolverIp = it },
                label = { Text("DNS Resolver Server") },
                placeholder = { Text("e.g. 1.1.1.1 or 8.8.8.8") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dns_resolver_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Fast Resolver Presets",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dnsPresets.forEach { (label, ip) ->
                    FilterChip(
                        selected = resolverIp == ip,
                        onClick = { resolverIp = ip },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = pubKey,
                onValueChange = { pubKey = it },
                label = { Text("DNSTT Public Key (Hex)") },
                placeholder = { Text("Public key for packet encryption") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dns_pubkey_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Transport Mode",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("UDP (Port 53)", "DoH (Port 443)", "DoT (Port 853)").forEach { q ->
                    FilterChip(
                        selected = queryType == q,
                        onClick = { queryType = q },
                        label = { Text(q, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VpnRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val updated = config.copy(
                        dnsNameserver = nsDomain,
                        dnsResolver = resolverIp,
                        dnsPublicKey = pubKey,
                        connectionMode = ConnectionMode.DNS_TUNNEL,
                        isCustomSetupEnabled = true
                    )
                    viewModel.updateConfig(updated)
                    Toast.makeText(context, "DNS Tunnel configuration applied!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_dns_tunnel_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Apply DNS Tunnel", fontWeight = FontWeight.Bold)
            }
        }
    }
}
