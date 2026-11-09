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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
fun HostSniScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.config.collectAsState()
    val context = LocalContext.current

    var hostHeader by remember { mutableStateOf(config.customHostHeader) }
    var sniHost by remember { mutableStateOf(config.customSni) }
    var tlsVersion by remember { mutableStateOf(config.tlsVersion) }
    var enableSslProxy by remember { mutableStateOf(config.enableSslProxy) }

    val popularPresets = listOf(
        "cloudflare.com",
        "aws.amazon.com",
        "fastly.net",
        "zoom.us",
        "edgekey.net",
        "google.com"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Host & SNI Configuration",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("host_sni_back_button")) {
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
            // SNI Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = VpnRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom SNI (SSL/TLS)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnRedPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Server Name Indication hostname for TLS handshakes. Used to route and disguise SSL tunnel traffic.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sniHost,
                onValueChange = { sniHost = it },
                label = { Text("Server Name Indication (SNI)") },
                placeholder = { Text("e.g. cloudflare.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sni_input_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // TLS Version Chips
            Text(
                text = "TLS Version",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TLS 1.3", "TLS 1.2", "Auto").forEach { ver ->
                    FilterChip(
                        selected = tlsVersion == ver,
                        onClick = { tlsVersion = ver },
                        label = { Text(ver) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VpnRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SSL Proxy Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable SSL/TLS Proxy",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Wrap TCP tunnel stream in secure SSL layer",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableSslProxy,
                    onCheckedChange = { enableSslProxy = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VpnRedPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Host Header Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = VpnRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Custom Host Header (HTTP Mode)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VpnRedPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The Host header sent in HTTP proxy requests to bypass zero-rated domains.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = hostHeader,
                onValueChange = { hostHeader = it },
                label = { Text("HTTP Host Header") },
                placeholder = { Text("e.g. m.facebook.com or cloudflare.net") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("host_header_input_field"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Presets
            Text(
                text = "Common Domain Presets",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                popularPresets.forEach { preset ->
                    FilterChip(
                        selected = sniHost == preset,
                        onClick = {
                            sniHost = preset
                            hostHeader = preset
                        },
                        label = { Text(preset, fontSize = 11.5.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val updated = config.copy(
                        customHostHeader = hostHeader,
                        customSni = sniHost,
                        tlsVersion = tlsVersion,
                        enableSslProxy = enableSslProxy,
                        isCustomSetupEnabled = true
                    )
                    viewModel.updateConfig(updated)
                    Toast.makeText(context, "Host & SNI configuration saved!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_host_sni_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VpnRedPrimary)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Apply Configuration", fontWeight = FontWeight.Bold)
            }
        }
    }
}
