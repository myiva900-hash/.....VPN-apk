package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VpnRepository
import com.example.model.AppSettings
import com.example.model.ConnectionMode
import com.example.model.LogEntry
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.model.VpnConnectionState
import com.example.model.VpnServer
import com.example.vpn.MutigandaVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    LOGS,
    SETTINGS,
    PAYLOAD_GEN,
    HOST_SNI,
    DNS_TUNNEL,
    SERVERS,
    IMPORT_EXPORT,
    ABOUT
}

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VpnRepository.getInstance(application)

    val connectionState: StateFlow<VpnConnectionState> = repository.connectionState
    val trafficStats: StateFlow<TrafficStats> = repository.trafficStats
    val logs: StateFlow<List<LogEntry>> = repository.logs
    val config: StateFlow<VpnConfig> = repository.config
    val settings: StateFlow<AppSettings> = repository.settings
    val servers: StateFlow<List<VpnServer>> = repository.servers
    val connectionDurationSeconds: StateFlow<Long> = repository.connectionDurationSeconds

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<AppScreen>()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            true
        } else {
            false
        }
    }

    fun onConnectClick(onPrepareVpn: (Intent) -> Unit) {
        val app = getApplication<Application>()
        if (connectionState.value == VpnConnectionState.CONNECTED ||
            connectionState.value == VpnConnectionState.CONNECTING ||
            connectionState.value == VpnConnectionState.AUTHENTICATING ||
            connectionState.value == VpnConnectionState.ESTABLISHING_TUNNEL
        ) {
            MutigandaVpnService.stopVpn(app)
        } else {
            val vpnIntent = VpnService.prepare(app)
            if (vpnIntent != null) {
                onPrepareVpn(vpnIntent)
            } else {
                MutigandaVpnService.startVpn(app)
            }
        }
    }

    fun onVpnPermissionGranted() {
        MutigandaVpnService.startVpn(getApplication())
    }

    fun setConnectionMode(mode: ConnectionMode) {
        repository.setConnectionMode(mode)
    }

    fun setServer(server: VpnServer) {
        repository.setServer(server)
    }

    fun setPort(port: Int) {
        repository.setPort(port)
    }

    fun setCustomSetup(enabled: Boolean) {
        repository.setCustomSetup(enabled)
    }

    fun updateConfig(newConfig: VpnConfig) {
        repository.updateConfig(newConfig)
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.updateSettings(newSettings)
    }

    fun clearLogs() {
        repository.clearLogs()
    }

    fun addCustomServer(name: String, ip: String, location: String, countryCode: String, port: Int): Boolean {
        return repository.addCustomServer(name, ip, location, countryCode, port)
    }

    fun removeServer(serverId: String) {
        repository.removeServer(serverId)
    }

    fun exportConfig(note: String, passwordLocked: Boolean): String {
        return repository.exportConfigJson(note, passwordLocked)
    }

    fun importConfig(jsonString: String): Result<String> {
        return repository.importConfigJson(jsonString)
    }
}
