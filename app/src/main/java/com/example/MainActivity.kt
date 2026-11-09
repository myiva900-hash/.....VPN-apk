package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.AppScreen
import com.example.ui.VpnViewModel
import com.example.ui.components.AppTopBar
import com.example.ui.components.NavDrawerContent
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AddCustomServerScreen
import com.example.ui.screens.DnsTunnelScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HostSniScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.LogScreen
import com.example.ui.screens.PayloadGeneratorScreen
import com.example.ui.screens.ServerManagementScreen
import com.example.ui.screens.ServerValidationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MutigandaVpnTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            // VPN Permission launcher for VpnService.prepare(context)
            val vpnLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.onVpnPermissionGranted()
                } else {
                    viewModel.clearLogs()
                    viewModel.navigateTo(AppScreen.HOME)
                }
            }

            // Notification permission request for Android 13+
            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            MutigandaVpnTheme(themePreference = settings.theme) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        NavDrawerContent(
                            currentScreen = currentScreen,
                            onNavigate = { screen ->
                                viewModel.navigateTo(screen)
                            },
                            onCloseDrawer = {
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    if (currentScreen == AppScreen.HOME) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                AppTopBar(
                                    onMenuClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    },
                                    onSettingsClick = {
                                        viewModel.navigateTo(AppScreen.SETTINGS)
                                    },
                                    onNavigate = { screen ->
                                        viewModel.navigateTo(screen)
                                    },
                                    onClearLogs = {
                                        viewModel.clearLogs()
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onNavigate = { screen -> viewModel.navigateTo(screen) },
                                    onPrepareVpn = { intent -> vpnLauncher.launch(intent) }
                                )
                            }
                        }
                    } else {
                        // Subscreens with their own custom TopAppBars and BackHandler
                        when (currentScreen) {
                            AppScreen.LOGS -> LogScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.PAYLOAD_GEN -> PayloadGeneratorScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.HOST_SNI -> HostSniScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.DNS_TUNNEL -> DnsTunnelScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SERVERS -> ServerManagementScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.ADD_CUSTOM_SERVER -> AddCustomServerScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.SERVER_VALIDATION -> ServerValidationScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.IMPORT_EXPORT -> ImportExportScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateBack() }
                            )
                            AppScreen.ABOUT -> AboutScreen(
                                onBack = { viewModel.navigateBack() }
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
