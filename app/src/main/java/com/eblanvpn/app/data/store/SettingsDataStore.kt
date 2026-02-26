package com.eblanvpn.app.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.eblanvpn.app.data.model.AccentColor
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.RoutingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("app_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DNS1 = stringPreferencesKey("dns1")
        val DNS2 = stringPreferencesKey("dns2")
        val DNS_OVER_TLS = booleanPreferencesKey("dns_over_tls")
        val BYPASS_LAN = booleanPreferencesKey("bypass_lan")
        val ENABLE_IPV6 = booleanPreferencesKey("enable_ipv6")
        val MTU = intPreferencesKey("mtu")
        val LOCAL_SOCKS_PORT = intPreferencesKey("local_socks_port")
        val LOCAL_HTTP_PORT = intPreferencesKey("local_http_port")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val SHOW_NOTIFICATION_TRAFFIC = booleanPreferencesKey("show_notification_traffic")
        val LOG_LEVEL = stringPreferencesKey("log_level")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            AppSettings(
                accentColor = prefs[Keys.ACCENT_COLOR]?.let {
                    runCatching { AccentColor.valueOf(it) }.getOrDefault(AccentColor.PURPLE)
                } ?: AccentColor.PURPLE,
                useDarkTheme = prefs[Keys.DARK_THEME] ?: true,
                dns1 = prefs[Keys.DNS1] ?: "1.1.1.1",
                dns2 = prefs[Keys.DNS2] ?: "8.8.8.8",
                enableDnsOverTls = prefs[Keys.DNS_OVER_TLS] ?: false,
                enableBypassLan = prefs[Keys.BYPASS_LAN] ?: true,
                enableIpv6 = prefs[Keys.ENABLE_IPV6] ?: false,
                mtu = prefs[Keys.MTU] ?: 1500,
                localSocksPort = prefs[Keys.LOCAL_SOCKS_PORT] ?: 10808,
                localHttpPort = prefs[Keys.LOCAL_HTTP_PORT] ?: 10809,
                routingMode = prefs[Keys.ROUTING_MODE]?.let {
                    runCatching { RoutingMode.valueOf(it) }.getOrDefault(RoutingMode.GLOBAL)
                } ?: RoutingMode.GLOBAL,
                autoConnect = prefs[Keys.AUTO_CONNECT] ?: false,
                showNotificationTraffic = prefs[Keys.SHOW_NOTIFICATION_TRAFFIC] ?: true,
                logLevel = prefs[Keys.LOG_LEVEL] ?: "warning"
            )
        }

    suspend fun updateSettings(block: AppSettings.() -> AppSettings) {
        val current = settings.let { flow ->
            var result = AppSettings()
            // We just update the dataStore directly
            result
        }
        context.dataStore.edit { prefs ->
            // We'll handle it differently - update each key individually
        }
    }

    suspend fun setAccentColor(color: AccentColor) =
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = color.name }

    suspend fun setDarkTheme(dark: Boolean) =
        context.dataStore.edit { it[Keys.DARK_THEME] = dark }

    suspend fun setDns1(dns: String) =
        context.dataStore.edit { it[Keys.DNS1] = dns }

    suspend fun setDns2(dns: String) =
        context.dataStore.edit { it[Keys.DNS2] = dns }

    suspend fun setDnsOverTls(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DNS_OVER_TLS] = enabled }

    suspend fun setBypassLan(enabled: Boolean) =
        context.dataStore.edit { it[Keys.BYPASS_LAN] = enabled }

    suspend fun setEnableIpv6(enabled: Boolean) =
        context.dataStore.edit { it[Keys.ENABLE_IPV6] = enabled }

    suspend fun setMtu(mtu: Int) =
        context.dataStore.edit { it[Keys.MTU] = mtu }

    suspend fun setLocalSocksPort(port: Int) =
        context.dataStore.edit { it[Keys.LOCAL_SOCKS_PORT] = port }

    suspend fun setLocalHttpPort(port: Int) =
        context.dataStore.edit { it[Keys.LOCAL_HTTP_PORT] = port }

    suspend fun setRoutingMode(mode: RoutingMode) =
        context.dataStore.edit { it[Keys.ROUTING_MODE] = mode.name }

    suspend fun setAutoConnect(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_CONNECT] = enabled }

    suspend fun setShowNotificationTraffic(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SHOW_NOTIFICATION_TRAFFIC] = enabled }

    suspend fun setLogLevel(level: String) =
        context.dataStore.edit { it[Keys.LOG_LEVEL] = level }
}
