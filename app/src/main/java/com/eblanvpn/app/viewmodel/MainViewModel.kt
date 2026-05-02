package com.eblanvpn.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.data.model.VpnState
import com.eblanvpn.app.data.repository.VpnRepository
import com.eblanvpn.app.service.EblanVpnService
import com.eblanvpn.app.utils.VlessParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: VpnRepository
) : AndroidViewModel(application) {

    // ─── VPN State (from service) ─────────────────────────────────────────────

    val vpnState: StateFlow<VpnState> = EblanVpnService.vpnState
        .stateIn(viewModelScope, SharingStarted.Eagerly, VpnState.DISCONNECTED)

    val trafficStats: StateFlow<TrafficStats> = EblanVpnService.trafficStats
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrafficStats())

    val connectedServer: StateFlow<ServerConfig?> = EblanVpnService.connectedServer
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val connectionTime: StateFlow<Long> = EblanVpnService.connectionTime
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // ─── Server List ──────────────────────────────────────────────────────────

    val servers: StateFlow<List<ServerConfig>> = repository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedServer: StateFlow<ServerConfig?> = repository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ─── Settings ─────────────────────────────────────────────────────────────

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // ─── UI State ─────────────────────────────────────────────────────────────

    private val _showPermissionRequest = MutableStateFlow(false)
    val showPermissionRequest: StateFlow<Boolean> = _showPermissionRequest.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun emitSnackbar(message: String) {
        viewModelScope.launch { _snackbarMessage.emit(message) }
    }

    private var pendingVpnPermission = false

    // ─── VPN Control ──────────────────────────────────────────────────────────

    fun toggleConnection(onPermissionNeeded: (Intent) -> Unit) {
        when (vpnState.value) {
            VpnState.CONNECTED, VpnState.CONNECTING -> disconnect()
            VpnState.DISCONNECTED, VpnState.ERROR -> connect(onPermissionNeeded)
            VpnState.DISCONNECTING -> { /* wait */ }
        }
    }

    fun connect(onPermissionNeeded: (Intent) -> Unit) {
        val server = selectedServer.value ?: run {
            viewModelScope.launch { _snackbarMessage.emit("Выберите сервер для подключения") }
            return
        }
        val ctx = getApplication<Application>()

        // Check VPN permission
        val permIntent = VpnService.prepare(ctx)
        if (permIntent != null) {
            pendingVpnPermission = true
            onPermissionNeeded(permIntent)
            return
        }

        doConnect(server)
    }

    fun onVpnPermissionGranted() {
        if (pendingVpnPermission) {
            pendingVpnPermission = false
            val server = selectedServer.value ?: return
            doConnect(server)
        }
    }

    fun onVpnPermissionDenied() {
        pendingVpnPermission = false
        viewModelScope.launch {
            _snackbarMessage.emit("Разрешение VPN отклонено")
        }
    }

    private fun doConnect(server: ServerConfig) {
        val ctx = getApplication<Application>()
        val currentSettings = settings.value
        EblanVpnService.startVpn(ctx, server, currentSettings)
    }

    fun disconnect() {
        val ctx = getApplication<Application>()
        EblanVpnService.stopVpn(ctx)
    }

    // ─── Server Management ────────────────────────────────────────────────────

    fun selectServer(server: ServerConfig) {
        viewModelScope.launch {
            repository.selectServer(server.id)
        }
    }

    fun addServer(config: ServerConfig) {
        viewModelScope.launch {
            val id = repository.addServer(config)
            if (servers.value.isEmpty()) {
                repository.selectServer(id)
            }
            _snackbarMessage.emit("Сервер «${config.name}» добавлен")
        }
    }

    fun updateServer(config: ServerConfig) {
        viewModelScope.launch {
            repository.updateServer(config)
        }
    }

    fun deleteServer(config: ServerConfig) {
        viewModelScope.launch {
            repository.deleteServer(config)
            _snackbarMessage.emit("Сервер удалён")
        }
    }

    fun importFromClipboard(text: String) {
        viewModelScope.launch {
            val servers = VlessParser.parseMultiple(text)
            if (servers.isEmpty()) {
                _snackbarMessage.emit("Не удалось найти конфигурации в буфере обмена")
                return@launch
            }
            var firstId = -1L
            servers.forEach { config ->
                val id = repository.addServer(config)
                if (firstId == -1L) firstId = id
            }
            if (this@MainViewModel.servers.value.size == servers.size) {
                // These are the first servers — select the first one
                repository.selectServer(firstId)
            }
            _snackbarMessage.emit("Импортировано: ${servers.size} конфигураций")
        }
    }

    fun importSingleLink(link: String): Boolean {
        val config = VlessParser.parse(link) ?: return false
        addServer(config)
        return true
    }
}
