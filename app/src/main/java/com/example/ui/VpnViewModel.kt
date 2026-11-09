package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VpnRepository
import com.example.data.local.ServerValidationResult
import com.example.model.AppSettings
import com.example.model.ConnectionMode
import com.example.model.LogEntry
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.data.local.CustomServerProfile
import com.example.data.security.KeystoreEncryptionHelper
import com.example.model.VpnConnectionState
import com.example.model.VpnServer
import com.example.vpn.MutigandaVpnService
import kotlinx.coroutines.flow.Flow
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
    ADD_CUSTOM_SERVER,
    SERVER_VALIDATION,
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

    val validationHistory: Flow<List<ServerValidationResult>> = repository.validationHistory
    val customProfiles: Flow<List<CustomServerProfile>> = repository.customProfiles

    private val _editingProfile = MutableStateFlow<CustomServerProfile?>(null)
    val editingProfile: StateFlow<CustomServerProfile?> = _editingProfile.asStateFlow()

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
        val state = connectionState.value
        if (state == VpnConnectionState.CONNECTED ||
            state == VpnConnectionState.CONNECTING ||
            state == VpnConnectionState.AUTHENTICATING ||
            state == VpnConnectionState.ESTABLISHING_TUNNEL
        ) {
            repository.isManualDisconnect = true
            MutigandaVpnService.stopVpn(app)
        } else {
            repository.isManualDisconnect = false
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

    fun saveValidationResult(result: ServerValidationResult) {
        viewModelScope.launch {
            repository.saveValidation(result)
        }
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

    fun openAddServer() {
        _editingProfile.value = null
        navigateTo(AppScreen.ADD_CUSTOM_SERVER)
    }

    fun openEditServer(profile: CustomServerProfile) {
        _editingProfile.value = profile
        navigateTo(AppScreen.ADD_CUSTOM_SERVER)
    }

    fun saveCustomServerProfile(
        id: String?,
        serverName: String,
        hostOrIp: String,
        port: Int,
        username: String,
        passwordPlain: String,
        vpnProtocol: String,
        location: String,
        countryCode: String,
        flag: String,
        configFileContent: String,
        publicKey: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val trimmedHost = hostOrIp.trim()
        if (serverName.isBlank()) {
            onComplete(false, "Server Name is required.")
            return
        }
        if (trimmedHost.isBlank()) {
            onComplete(false, "IP Address or Hostname is required.")
            return
        }
        if (port !in 1..65535) {
            onComplete(false, "Port must be a valid number between 1 and 65535.")
            return
        }

        val encryptedPassword = if (passwordPlain.isNotBlank()) {
            KeystoreEncryptionHelper.encrypt(passwordPlain)
        } else {
            _editingProfile.value?.encryptedPassword ?: ""
        }

        val profileId = id ?: "custom_${System.currentTimeMillis()}"
        val profile = CustomServerProfile(
            id = profileId,
            serverName = serverName.trim(),
            hostOrIp = trimmedHost,
            port = port,
            username = username.trim(),
            encryptedPassword = encryptedPassword,
            vpnProtocol = vpnProtocol,
            location = location.trim().ifBlank { "Custom Node" },
            countryCode = countryCode.ifBlank { "CUSTOM" },
            flag = flag.ifBlank { "🛡️" },
            configFileContent = configFileContent.trim(),
            publicKey = publicKey.trim(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (id != null) {
                repository.updateCustomProfile(profile)
            } else {
                repository.saveCustomProfile(profile)
            }
            _editingProfile.value = null
            onComplete(true, "Server profile saved successfully!")
        }
    }

    fun deleteCustomProfile(id: String) {
        viewModelScope.launch {
            repository.deleteCustomProfile(id)
        }
    }

    fun exportConfig(note: String, passwordLocked: Boolean): String {
        return repository.exportConfigJson(note, passwordLocked)
    }

    fun importConfig(jsonString: String): Result<String> {
        return repository.importConfigJson(jsonString)
    }
}
