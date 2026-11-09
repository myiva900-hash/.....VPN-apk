package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VpnViewModel
import com.example.ui.theme.VpnRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val settings by viewModel.settings.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
            // Notification Section
            SettingsSectionHeader(title = "Notification")

            SettingsSwitchItem(
                title = "Sound",
                subtitle = "Notify with a sound while connecting or disconnecting.",
                checked = settings.sound,
                testTag = "setting_sound_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(sound = it)) }
            )

            SettingsSwitchItem(
                title = "Vibrate",
                subtitle = "Notify with a vibration while connecting or disconnecting.",
                checked = settings.vibrate,
                testTag = "setting_vibrate_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(vibrate = it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Section
            SettingsSectionHeader(title = "Appearance")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(vertical = 12.dp)
                    .testTag("setting_theme_picker")
            ) {
                Text(
                    text = "Choose Theme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = settings.theme,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connection Section
            SettingsSectionHeader(title = "Connection")

            SettingsSwitchItem(
                title = "Connection Retry",
                subtitle = "Try to reconnect after connection is lost. By default, service is stopped after 10 consecutive unsuccessful attempts.",
                checked = settings.connectionRetry,
                testTag = "setting_retry_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(connectionRetry = it)) }
            )

            SettingsSwitchItem(
                title = "Forward DNS",
                subtitle = "Forward DNS queries to remote server if enabled.",
                checked = settings.forwardDns,
                testTag = "setting_forward_dns_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(forwardDns = it)) }
            )

            SettingsSwitchItem(
                title = "Custom DNS",
                subtitle = "Use custom DNS servers to resolve DNS queries.",
                checked = settings.customDns,
                testTag = "setting_custom_dns_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(customDns = it)) }
            )

            AnimatedVisibility(visible = settings.customDns) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = settings.primaryDns,
                        onValueChange = { viewModel.updateSettings(settings.copy(primaryDns = it)) },
                        label = { Text("Primary DNS") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("primary_dns_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.secondaryDns,
                        onValueChange = { viewModel.updateSettings(settings.copy(secondaryDns = it)) },
                        label = { Text("Secondary DNS") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secondary_dns_input"),
                        singleLine = true
                    )
                }
            }

            SettingsSwitchItem(
                title = "Auto Reconnect",
                subtitle = "Automatically reconnect if network interface changes (Wi-Fi / Mobile Data).",
                checked = settings.autoReconnect,
                testTag = "setting_auto_reconnect_switch",
                onCheckedChange = { viewModel.updateSettings(settings.copy(autoReconnect = it)) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Connection Timeout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${settings.connectionTimeoutSeconds} seconds",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Dark", "Light", "System Default").forEach { themeName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(theme = themeName))
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = settings.theme == themeName,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(theme = themeName))
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = VpnRedPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = themeName, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = VpnRedPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = VpnRedPrimary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VpnRedPrimary
            )
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}
