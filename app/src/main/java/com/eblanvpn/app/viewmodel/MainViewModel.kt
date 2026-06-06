package com.eblanvpn.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.Subscription
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

    // ─── Subscriptions ────────────────────────────────────────────────────────

    val subscriptions: StateFlow<List<Subscription>> = repository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** ids of subscriptions currently being refreshed (for spinners) */
    private val _refreshingSubs = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingSubs: StateFlow<Set<Long>> = _refreshingSubs.asStateFlow()

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

    // ─── Subscription Management ──────────────────────────────────────────────

    /** Add a new subscription and immediately download its servers. */
    fun addSubscription(name: String, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            emitSnackbar("Введите ссылку подписки")
            return
        }
        viewModelScope.launch {
            val draft = Subscription(name = name.trim(), url = cleanUrl)
            val id = repository.addSubscription(draft)
            refreshInternal(draft.copy(id = id), announce = true)
        }
    }

    fun refreshSubscription(subscription: Subscription) {
        viewModelScope.launch { refreshInternal(subscription, announce = true) }
    }

    fun refreshAllSubscriptions() {
        viewModelScope.launch {
            val subs = subscriptions.value
            if (subs.isEmpty()) {
                emitSnackbar("Нет подписок")
                return@launch
            }
            var total = 0
            subs.forEach { sub -> total += refreshInternal(sub, announce = false) }
            emitSnackbar("Обновлено подписок: ${subs.size} • серверов: $total")
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
            emitSnackbar("Подписка удалена")
        }
    }

    private suspend fun refreshInternal(subscription: Subscription, announce: Boolean): Int {
        _refreshingSubs.update { it + subscription.id }
        val result = repository.refreshSubscription(subscription)
        _refreshingSubs.update { it - subscription.id }

        var imported = 0
        result.onSuccess { count ->
            imported = count
            if (announce) emitSnackbar("«${subscription.displayName}»: импортировано $count серверов")
        }.onFailure { e ->
            if (announce) emitSnackbar("Ошибка подписки: ${e.message ?: "сбой загрузки"}")
        }

        // If nothing is selected yet, pick the first available server
        if (selectedServer.value == null) {
            servers.value.firstOrNull()?.let { repository.selectServer(it.id) }
        }
        return imported
    }
}
