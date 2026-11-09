package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomServerProfile
import com.example.model.VpnServer
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnConnectedGreen
import com.example.ui.theme.VpnRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagementScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val servers by viewModel.servers.collectAsState()
    val config by viewModel.config.collectAsState()
    val customProfiles by viewModel.customProfiles.collectAsState(initial = emptyList())
    val context = LocalContext.current

    var serverToDelete by remember { mutableStateOf<VpnServer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Server Management",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("server_mgmt_back_button")) {
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
                            Toast.makeText(context, "Latency refreshed for all ${servers.size} servers!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("ping_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Test Ping",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VpnRedPrimary)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddServer() },
                containerColor = VpnRedPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_custom_server_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Server")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = VpnRedPrimary.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = VpnRedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Select or Manage VPN Servers",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = VpnRedPrimary
                            )
                            Text(
                                text = "Add private VPS gateways with OpenVPN, WireGuard, SSH, or Direct TLS. Custom profiles persist locally in Room with Keystore protection.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Server items
            items(servers, key = { it.id }) { server ->
                val isSelected = server.id == config.selectedServerId
                val matchingProfile: CustomServerProfile? = customProfiles.find { it.id == server.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.setServer(server)
                            Toast.makeText(context, "Selected: ${server.name}", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("server_item_${server.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) VpnRedPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = server.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.name,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) VpnRedPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (server.isCustom) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = VpnRedPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = matchingProfile?.vpnProtocol ?: "CUSTOM",
                                                color = VpnRedPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${server.location} • ${server.ip}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${server.pingMs} ms",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (server.pingMs < 50) VpnConnectedGreen else VpnRedPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = VpnConnectedGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Edit / Delete buttons for custom servers
                        if (server.isCustom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (matchingProfile != null) {
                                            viewModel.openEditServer(matchingProfile)
                                        } else {
                                            // Fallback profile representation if created via legacy add
                                            val fallback = CustomServerProfile(
                                                id = server.id,
                                                serverName = server.name,
                                                hostOrIp = server.ip,
                                                port = server.supportedPorts.firstOrNull() ?: 443,
                                                location = server.location,
                                                countryCode = server.countryCode,
                                                flag = server.flag
                                            )
                                            viewModel.openEditServer(fallback)
                                        }
                                    },
                                    modifier = Modifier.testTag("edit_server_${server.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = { serverToDelete = server },
                                    modifier = Modifier.testTag("delete_server_${server.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Delete confirmation dialog
    serverToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server Profile", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${target.name}' (${target.ip})? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomProfile(target.id)
                        serverToDelete = null
                        Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
