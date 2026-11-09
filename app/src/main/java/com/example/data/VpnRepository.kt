package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.CustomServerProfile
import com.example.data.local.ServerValidationResult
import com.example.data.local.VpnDatabase
import com.example.data.security.KeystoreEncryptionHelper
import com.example.model.AppSettings
import com.example.model.ConnectionMode
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.TrafficStats
import com.example.model.VpnConfig
import com.example.model.VpnConnectionState
import com.example.model.VpnServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class VpnRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("mutiganda_vpn_prefs", Context.MODE_PRIVATE)

    private val db = VpnDatabase.getInstance(appContext)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(level = LogLevel.INFO, message = "Mutiganda Yvan VPN initialized."),
            LogEntry(level = LogLevel.INFO, message = "Custom Server Manager & Android Keystore encryption active.")
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<VpnConfig> = _config.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _servers = MutableStateFlow<List<VpnServer>>(VpnServer.DEFAULT_SERVERS)
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _connectionDurationSeconds = MutableStateFlow(0L)
    val connectionDurationSeconds: StateFlow<Long> = _connectionDurationSeconds.asStateFlow()

    // Flag to differentiate between user-initiated disconnect and network drops
    @Volatile
    var isManualDisconnect: Boolean = false

    val validationHistory: Flow<List<ServerValidationResult>> =
        db.serverValidationDao().getAllValidations()

    val customProfiles: Flow<List<CustomServerProfile>> =
        db.customServerProfileDao().getAllServerProfiles()

    init {
        // Collect custom servers from Room and merge reactively with default server locations
        repositoryScope.launch {
            db.customServerProfileDao().getAllServerProfiles().collect { profiles ->
                val customServers = profiles.map { it.toVpnServer() }
                _servers.value = VpnServer.DEFAULT_SERVERS + customServers
            }
        }
    }

    suspend fun saveCustomProfile(profile: CustomServerProfile) {
        db.customServerProfileDao().insertServerProfile(profile)
        addLog(
            LogLevel.INFO,
            "[PROFILE] Saved server profile '${profile.serverName}' (${profile.hostOrIp}:${profile.port}) [Protocol: ${profile.vpnProtocol}]. Credentials encrypted via Android Keystore."
        )
    }

    suspend fun updateCustomProfile(profile: CustomServerProfile) {
        db.customServerProfileDao().updateServerProfile(profile)
        addLog(
            LogLevel.INFO,
            "[PROFILE] Updated server profile '${profile.serverName}' (${profile.hostOrIp}:${profile.port})."
        )
    }

    suspend fun deleteCustomProfile(id: String) {
        db.customServerProfileDao().deleteServerProfile(id)
        if (_config.value.selectedServerId == id) {
            setServer(VpnServer.DEFAULT_SERVERS.first())
        }
        addLog(LogLevel.INFO, "[PROFILE] Deleted custom server profile $id from Room database.")
    }

    suspend fun getCustomProfileById(id: String): CustomServerProfile? {
        return db.customServerProfileDao().getServerProfileById(id)
    }

    suspend fun saveValidation(result: ServerValidationResult) {
        db.serverValidationDao().insertValidation(result)
    }

    suspend fun clearValidations() {
        db.serverValidationDao().clearAll()
    }

    fun updateConnectionState(state: VpnConnectionState) {
        _connectionState.value = state
        when (state) {
            VpnConnectionState.CONNECTING -> {
                _connectionDurationSeconds.value = 0L
                triggerFeedback(isConnect = true)
            }
            VpnConnectionState.AUTHENTICATING -> {
                _connectionDurationSeconds.value = 0L
            }
            VpnConnectionState.ESTABLISHING_TUNNEL -> {
                _connectionDurationSeconds.value = 0L
            }
            VpnConnectionState.CONNECTED -> {
                triggerFeedback(isConnect = true)
            }
            VpnConnectionState.DISCONNECTING -> {}
            VpnConnectionState.DISCONNECTED -> {
                _connectionDurationSeconds.value = 0L
                _trafficStats.value = TrafficStats()
                triggerFeedback(isConnect = false)
            }
            VpnConnectionState.FAILED -> {
                _connectionDurationSeconds.value = 0L
                _trafficStats.value = TrafficStats()
                triggerFeedback(isConnect = false)
            }
        }
    }

    fun updateTrafficStats(stats: TrafficStats) {
        _trafficStats.value = stats
    }

    fun updateDuration(seconds: Long) {
        if (seconds in 0..31536000L) {
            _connectionDurationSeconds.value = seconds
        } else {
            _connectionDurationSeconds.value = 0L
        }
    }

    fun addLog(level: LogLevel, message: String) {
        val entry = LogEntry(level = level, message = message)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 250) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(level = LogLevel.INFO, message = "Logs cleared by user.")
        )
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _config.value = _config.value.copy(connectionMode = mode)
        saveConfig(_config.value)
        addLog(LogLevel.INFO, "Connection mode set to: ${mode.title} (${mode.subtitle})")
    }

    fun setServer(server: VpnServer) {
        _config.value = _config.value.copy(
            selectedServerId = server.id,
            selectedPort = server.supportedPorts.firstOrNull() ?: _config.value.selectedPort
        )
        saveConfig(_config.value)
        addLog(LogLevel.INFO, "Selected server: ${server.name} [${server.flag}] -> ${server.ip}")
    }

    fun setPort(port: Int) {
        _config.value = _config.value.copy(selectedPort = port)
        saveConfig(_config.value)
        addLog(LogLevel.INFO, "Port set to: P $port")
    }

    fun setCustomSetup(enabled: Boolean) {
        _config.value = _config.value.copy(isCustomSetupEnabled = enabled)
        saveConfig(_config.value)
        addLog(LogLevel.INFO, "Custom setup ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    fun updateConfig(newConfig: VpnConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
        addLog(LogLevel.INFO, "VPN configuration profile updated.")
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
        addLog(LogLevel.INFO, "App preferences updated.")
    }

    fun addCustomServer(name: String, ip: String, location: String, countryCode: String, port: Int): Boolean {
        val profile = CustomServerProfile(
            id = "custom_" + System.currentTimeMillis(),
            serverName = name.ifBlank { "Custom Server" },
            hostOrIp = ip.trim().ifBlank { "127.0.0.1" },
            port = port,
            location = location.ifBlank { "User Defined" },
            countryCode = countryCode.ifBlank { "CUSTOM" },
            flag = "🛡️"
        )
        repositoryScope.launch {
            saveCustomProfile(profile)
        }
        return true
    }

    fun removeServer(serverId: String) {
        repositoryScope.launch {
            deleteCustomProfile(serverId)
        }
    }

    fun exportConfigJson(note: String = "", passwordLocked: Boolean = false): String {
        val cfg = _config.value
        val json = JSONObject().apply {
            put("app", "Mutiganda Yvan VPN")
            put("version", "1.0.0")
            put("timestamp", System.currentTimeMillis())
            put("note", note.ifBlank { "Exported from Mutiganda Yvan VPN" })
            put("mode", cfg.connectionMode.name)
            put("server_id", cfg.selectedServerId)
            put("port", cfg.selectedPort)
            put("custom_setup", cfg.isCustomSetupEnabled)
            put("payload", cfg.payloadString)
            put("host_header", cfg.customHostHeader)
            put("sni", cfg.customSni)
            put("tls_version", cfg.tlsVersion)
            put("dns_ns", cfg.dnsNameserver)
            put("dns_resolver", cfg.dnsResolver)
            put("dns_pubkey", cfg.dnsPublicKey)
            put("locked", passwordLocked)
        }
        return json.toString(2)
    }

    fun importConfigJson(jsonString: String): Result<String> {
        return try {
            val json = JSONObject(jsonString)
            val app = json.optString("app", "")
            if (!app.contains("Mutiganda", ignoreCase = true) && !json.has("mode")) {
                return Result.failure(IllegalArgumentException("Invalid config format: Mutiganda Yvan VPN parameters missing"))
            }
            val modeName = json.optString("mode", ConnectionMode.DIRECT.name)
            val mode = runCatching { ConnectionMode.valueOf(modeName) }.getOrDefault(ConnectionMode.DIRECT)
            val port = json.optInt("port", 80)
            val customSetup = json.optBoolean("custom_setup", true)
            val payload = json.optString("payload", _config.value.payloadString)
            val host = json.optString("host_header", _config.value.customHostHeader)
            val sni = json.optString("sni", _config.value.customSni)
            val tls = json.optString("tls_version", "TLS 1.3")
            val dnsNs = json.optString("dns_ns", _config.value.dnsNameserver)
            val dnsRes = json.optString("dns_resolver", _config.value.dnsResolver)
            val dnsKey = json.optString("dns_pubkey", _config.value.dnsPublicKey)
            val note = json.optString("note", "Imported Profile")

            val newCfg = _config.value.copy(
                connectionMode = mode,
                selectedPort = port,
                isCustomSetupEnabled = customSetup,
                payloadString = payload,
                customHostHeader = host,
                customSni = sni,
                tlsVersion = tls,
                dnsNameserver = dnsNs,
                dnsResolver = dnsRes,
                dnsPublicKey = dnsKey,
                importedConfigName = "Config_${System.currentTimeMillis() % 10000}",
                importedConfigNote = note
            )
            updateConfig(newCfg)
            addLog(LogLevel.INFO, "Config imported successfully: $note")
            Result.success("Config imported successfully!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun triggerFeedback(isConnect: Boolean) {
        val s = _settings.value
        if (s.vibrate) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isConnect) longArrayOf(0, 80, 50, 120) else longArrayOf(0, 100)
                    val amplitudes = if (isConnect) intArrayOf(0, 200, 0, 255) else intArrayOf(0, 180)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(if (isConnect) 150L else 100L)
                }
            }
        }
        if (s.sound) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                tone.startTone(if (isConnect) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_PROMPT, 150)
            }
        }
    }

    private fun loadConfig(): VpnConfig {
        val modeStr = prefs.getString("cfg_mode", ConnectionMode.DIRECT.name) ?: ConnectionMode.DIRECT.name
        val mode = runCatching { ConnectionMode.valueOf(modeStr) }.getOrDefault(ConnectionMode.DIRECT)
        return VpnConfig(
            selectedServerId = prefs.getString("cfg_server_id", "rw_gisozi") ?: "rw_gisozi",
            selectedPort = prefs.getInt("cfg_port", 80),
            connectionMode = mode,
            isCustomSetupEnabled = prefs.getBoolean("cfg_custom_setup", false),
            payloadString = prefs.getString("cfg_payload", VpnConfig().payloadString) ?: VpnConfig().payloadString,
            customHostHeader = prefs.getString("cfg_host", "cloudflare.com") ?: "cloudflare.com",
            customSni = prefs.getString("cfg_sni", "de-fra.vpn.mutigandayvan.net") ?: "de-fra.vpn.mutigandayvan.net",
            tlsVersion = prefs.getString("cfg_tls", "TLS 1.3") ?: "TLS 1.3",
            dnsNameserver = prefs.getString("cfg_dns_ns", "ns1.mutigandavpn.org") ?: "ns1.mutigandavpn.org",
            dnsResolver = prefs.getString("cfg_dns_res", "1.1.1.1") ?: "1.1.1.1"
        )
    }

    private fun saveConfig(cfg: VpnConfig) {
        prefs.edit()
            .putString("cfg_mode", cfg.connectionMode.name)
            .putString("cfg_server_id", cfg.selectedServerId)
            .putInt("cfg_port", cfg.selectedPort)
            .putBoolean("cfg_custom_setup", cfg.isCustomSetupEnabled)
            .putString("cfg_payload", cfg.payloadString)
            .putString("cfg_host", cfg.customHostHeader)
            .putString("cfg_sni", cfg.customSni)
            .putString("cfg_tls", cfg.tlsVersion)
            .putString("cfg_dns_ns", cfg.dnsNameserver)
            .putString("cfg_dns_res", cfg.dnsResolver)
            .apply()
    }

    private fun loadSettings(): AppSettings {
        return AppSettings(
            sound = prefs.getBoolean("set_sound", true),
            vibrate = prefs.getBoolean("set_vibrate", true),
            theme = prefs.getString("set_theme", "Dark") ?: "Dark",
            connectionRetry = prefs.getBoolean("set_retry", true),
            forwardDns = prefs.getBoolean("set_fwd_dns", false),
            customDns = prefs.getBoolean("set_custom_dns", false),
            primaryDns = prefs.getString("set_pri_dns", "1.1.1.1") ?: "1.1.1.1",
            secondaryDns = prefs.getString("set_sec_dns", "1.0.0.1") ?: "1.0.0.1",
            connectionTimeoutSeconds = prefs.getInt("set_timeout", 15),
            autoReconnect = prefs.getBoolean("set_auto_recon", false)
        )
    }

    private fun saveSettings(s: AppSettings) {
        prefs.edit()
            .putBoolean("set_sound", s.sound)
            .putBoolean("set_vibrate", s.vibrate)
            .putString("set_theme", s.theme)
            .putBoolean("set_retry", s.connectionRetry)
            .putBoolean("set_fwd_dns", s.forwardDns)
            .putBoolean("set_custom_dns", s.customDns)
            .putString("set_pri_dns", s.primaryDns)
            .putString("set_sec_dns", s.secondaryDns)
            .putInt("set_timeout", s.connectionTimeoutSeconds)
            .putBoolean("set_auto_recon", s.autoReconnect)
            .apply()
    }

    companion object {
        @Volatile
        private var instance: VpnRepository? = null

        fun getInstance(context: Context): VpnRepository {
            return instance ?: synchronized(this) {
                instance ?: VpnRepository(context).also { instance = it }
            }
        }
    }
}
