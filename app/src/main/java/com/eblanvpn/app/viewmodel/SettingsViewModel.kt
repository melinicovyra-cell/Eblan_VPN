package com.eblanvpn.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eblanvpn.app.data.model.AccentColor
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.PerAppMode
import com.eblanvpn.app.data.model.RoutingMode
import com.eblanvpn.app.data.repository.VpnRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val repository: VpnRepository
) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setAccentColor(color: AccentColor) {
        viewModelScope.launch {
            repository.settingsStore.setAccentColor(color)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setDarkTheme(dark)
        }
    }

    fun setDns1(dns: String) {
        viewModelScope.launch {
            repository.settingsStore.setDns1(dns)
        }
    }

    fun setDns2(dns: String) {
        viewModelScope.launch {
            repository.settingsStore.setDns2(dns)
        }
    }

    fun setDnsOverTls(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setDnsOverTls(enabled)
        }
    }

    fun setBypassLan(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setBypassLan(enabled)
        }
    }

    fun setEnableIpv6(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setEnableIpv6(enabled)
        }
    }

    fun setMtu(mtu: Int) {
        viewModelScope.launch {
            repository.settingsStore.setMtu(mtu)
        }
    }

    fun setLocalSocksPort(port: Int) {
        viewModelScope.launch {
            repository.settingsStore.setLocalSocksPort(port)
        }
    }

    fun setLocalHttpPort(port: Int) {
        viewModelScope.launch {
            repository.settingsStore.setLocalHttpPort(port)
        }
    }

    fun setRoutingMode(mode: RoutingMode) {
        viewModelScope.launch {
            repository.settingsStore.setRoutingMode(mode)
        }
    }

    fun setPerAppMode(mode: PerAppMode) {
        viewModelScope.launch {
            repository.settingsStore.setPerAppMode(mode)
        }
    }

    fun setPerAppList(packages: Set<String>) {
        viewModelScope.launch {
            repository.settingsStore.setPerAppList(packages)
        }
    }

    fun togglePerAppPackage(pkg: String) {
        viewModelScope.launch {
            val current = settings.value.perAppList
            val next = if (pkg in current) current - pkg else current + pkg
            repository.settingsStore.setPerAppList(next)
        }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setAutoConnect(enabled)
        }
    }

    fun setShowNotificationTraffic(enabled: Boolean) {
        viewModelScope.launch {
            repository.settingsStore.setShowNotificationTraffic(enabled)
        }
    }

    fun setLogLevel(level: String) {
        viewModelScope.launch {
            repository.settingsStore.setLogLevel(level)
        }
    }
}
