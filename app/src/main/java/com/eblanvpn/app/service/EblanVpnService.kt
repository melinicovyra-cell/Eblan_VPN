package com.eblanvpn.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.data.model.VpnState
import com.eblanvpn.app.utils.NotificationHelper
import com.eblanvpn.app.utils.V2RayConfigBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import libv2ray.Libv2ray
import libv2ray.V2RayVPNServiceSupportsSet

class EblanVpnService : VpnService(), V2RayVPNServiceSupportsSet {

    companion object {
        private const val TAG = "EblanVpnService"

        const val ACTION_START = "com.eblanvpn.START"
        const val ACTION_STOP = "com.eblanvpn.STOP"
        const val EXTRA_SERVER_CONFIG = "extra_server_config"
        const val EXTRA_APP_SETTINGS = "extra_app_settings"

        // Shared state accessible from UI
        val vpnState = MutableStateFlow(VpnState.DISCONNECTED)
        val trafficStats = MutableStateFlow(TrafficStats())
        val connectedServer = MutableStateFlow<ServerConfig?>(null)
        val connectionTime = MutableStateFlow(0L)

        fun startVpn(context: Context, server: ServerConfig, settings: AppSettings = AppSettings()) {
            val intent = Intent(context, EblanVpnService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SERVER_CONFIG, server)
            }
            context.startForegroundService(intent)
        }

        fun stopVpn(context: Context) {
            val intent = Intent(context, EblanVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var trafficMonitor: TrafficMonitor? = null
    private var connectionTimer: Job? = null
    private var currentServer: ServerConfig? = null
    private var currentSettings: AppSettings = AppSettings()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val server = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_SERVER_CONFIG, ServerConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_SERVER_CONFIG)
                }
                if (server != null) {
                    startVpnTunnel(server)
                } else {
                    Log.e(TAG, "No server config provided")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopVpnTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: ServerConfig) {
        if (vpnState.value.isActive) {
            Log.w(TAG, "VPN already active, stopping first")
            doStopVpn()
        }

        currentServer = server
        vpnState.value = VpnState.CONNECTING

        // Show connecting notification
        startForeground(
            NotificationHelper.VPN_NOTIFICATION_ID,
            NotificationHelper.buildConnectingNotification(this, server.name)
        )

        serviceScope.launch {
            try {
                val vpnFd = setupVpnInterface(server) ?: run {
                    Log.e(TAG, "Failed to establish VPN interface")
                    withContext(Dispatchers.Main) {
                        vpnState.value = VpnState.ERROR
                        stopSelf()
                    }
                    return@launch
                }

                val config = V2RayConfigBuilder.build(server, currentSettings)
                Log.d(TAG, "Starting v2ray core...")

                // Initialize v2ray
                Libv2ray.initV2Env(filesDir.absolutePath, "")

                val result = Libv2ray.startV2Ray(
                    this@EblanVpnService,
                    config,
                    null,
                    vpnFd.fd
                )

                if (result == 0L) {
                    withContext(Dispatchers.Main) {
                        vpnState.value = VpnState.CONNECTED
                        connectedServer.value = server
                        trafficStats.value = TrafficStats()
                    }

                    startTrafficMonitor()
                    startConnectionTimer()

                    // Update notification to connected state
                    NotificationHelper.updateVpnNotification(
                        this@EblanVpnService, server.name, TrafficStats()
                    )

                    Log.i(TAG, "VPN connected to ${server.name}")
                } else {
                    Log.e(TAG, "v2ray start failed with code: $result")
                    vpnFd.close()
                    withContext(Dispatchers.Main) {
                        vpnState.value = VpnState.ERROR
                        stopSelf()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting VPN", e)
                withContext(Dispatchers.Main) {
                    vpnState.value = VpnState.ERROR
                    doStopVpn()
                    stopSelf()
                }
            }
        }
    }

    private fun setupVpnInterface(server: ServerConfig): ParcelFileDescriptor? {
        return try {
            Builder().apply {
                setSession("Eblan VPN — ${server.name}")
                setMtu(currentSettings.mtu)

                // TUN address
                addAddress("198.18.0.1", 15)

                // DNS
                addDnsServer(currentSettings.dns1)
                if (currentSettings.dns2.isNotEmpty()) {
                    addDnsServer(currentSettings.dns2)
                }

                // Route all traffic
                addRoute("0.0.0.0", 0)
                if (currentSettings.enableIpv6) {
                    addRoute("::", 0)
                }

                // Allow bypass for specific apps
                allowFamily(android.system.OsConstants.AF_INET)
                if (currentSettings.enableIpv6) {
                    allowFamily(android.system.OsConstants.AF_INET6)
                }

                setConfigureIntent(
                    android.app.PendingIntent.getActivity(
                        this@EblanVpnService, 0,
                        packageManager.getLaunchIntentForPackage(packageName),
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }.establish()?.also {
                vpnInterface = it
                Log.d(TAG, "VPN interface established, fd=${it.fd}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup VPN interface", e)
            null
        }
    }

    private fun startTrafficMonitor() {
        trafficMonitor?.stop()
        trafficMonitor = TrafficMonitor(serviceScope) { stats ->
            trafficStats.value = stats
            // Update notification with new traffic stats
            currentServer?.let { server ->
                NotificationHelper.updateVpnNotification(
                    this@EblanVpnService,
                    server.name,
                    stats,
                    currentSettings.showNotificationTraffic
                )
            }
        }.also { it.start() }
    }

    private fun startConnectionTimer() {
        connectionTimer?.cancel()
        connectionTime.value = 0L
        connectionTimer = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                connectionTime.value++
            }
        }
    }

    private fun stopVpnTunnel() {
        vpnState.value = VpnState.DISCONNECTING
        serviceScope.launch {
            doStopVpn()
            withContext(Dispatchers.Main) {
                stopSelf()
            }
        }
    }

    private fun doStopVpn() {
        try {
            trafficMonitor?.stop()
            trafficMonitor = null

            connectionTimer?.cancel()
            connectionTimer = null

            runCatching { Libv2ray.stopV2Ray() }

            vpnInterface?.close()
            vpnInterface = null

            vpnState.value = VpnState.DISCONNECTED
            connectedServer.value = null
            connectionTime.value = 0L
            trafficStats.value = TrafficStats()

            Log.i(TAG, "VPN disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN", e)
            vpnState.value = VpnState.DISCONNECTED
        }
    }

    override fun onDestroy() {
        doStopVpn()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onRevoke() {
        Log.w(TAG, "VPN revoked by system")
        stopVpnTunnel()
        super.onRevoke()
    }

    // ─── V2RayVPNServiceSupportsSet ────────────────────────────────────────────

    override fun shutdown() {
        Log.d(TAG, "libv2ray requested shutdown")
        stopVpnTunnel()
    }

    override fun prepare() {
        // Called before starting — nothing needed here since we establish in setupVpnInterface
    }

    override fun protect(socket: Int): Boolean {
        return super.protect(socket).also {
            if (!it) Log.w(TAG, "Failed to protect socket: $socket")
        }
    }

    override fun onEmitStatus(duration: Long, status: String) {
        Log.d(TAG, "v2ray status [$duration]: $status")
    }

    override fun setup(it: String) {
        // libv2ray may call this to reconfigure the TUN interface
        // The format is a comma-separated config string: m,<mtu>,s,<addr>,<prefix>,d,<dns>,...
        Log.d(TAG, "v2ray setup: $it")
    }
}
