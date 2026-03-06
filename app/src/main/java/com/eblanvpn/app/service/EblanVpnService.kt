package com.eblanvpn.app.service
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
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

class EblanVpnService : VpnService(), CoreCallbackHandler {
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

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())    private var vpnInterface: ParcelFileDescriptor? = null
    private var trafficMonitor: TrafficMonitor? = null
    private var connectionTimer: Job? = null
    private var currentServer: ServerConfig? = null
    private var currentSettings: AppSettings = AppSettings()
    private var coreController: CoreController? = null

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
            ACTION_STOP -> stopVpnTunnel()
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
        startForeground(
            NotificationHelper.VPN_NOTIFICATION_ID,
            NotificationHelper.buildConnectingNotification(this, server.name)
        )
        serviceScope.launch {
            try {
                val vpnFd = setupVpnInterface(server) ?: run {
                    Log.e(TAG, "Failed to establish VPN interface")
                    withContext(Dispatchers.Main) {                        vpnState.value = VpnState.ERROR
                        stopSelf()
                    }
                    return@launch
                }
                val config = V2RayConfigBuilder.build(server, currentSettings)
                Log.d(TAG, "Starting xray-core...")
                // Initialize xray-core environment
                Libv2ray.initCoreEnv(filesDir.absolutePath, "")
                // Create controller and start the tunnel
                val controller = Libv2ray.newCoreController(this@EblanVpnService)
                coreController = controller
                controller.startLoop(config, vpnFd.fd)
                withContext(Dispatchers.Main) {
                    vpnState.value = VpnState.CONNECTED
                    connectedServer.value = server
                    trafficStats.value = TrafficStats()
                }
                startTrafficMonitor()
                startConnectionTimer()
                NotificationHelper.updateVpnNotification(
                    this@EblanVpnService, server.name, TrafficStats()
                )
                Log.i(TAG, "VPN connected to ${server.name}")
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
                addAddress("198.18.0.1", 15)
                addDnsServer(currentSettings.dns1)
                if (currentSettings.dns2.isNotEmpty()) {
                    addDnsServer(currentSettings.dns2)
                }
                addRoute("0.0.0.0", 0)
                if (currentSettings.enableIpv6) {
                    addRoute("::", 0)
                }
                allowFamily(android.system.OsConstants.AF_INET)                if (currentSettings.enableIpv6) {
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
            doStopVpn()            withContext(Dispatchers.Main) {
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
            runCatching { coreController?.stopLoop() }
            coreController = null
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

    // ─── CoreCallbackHandler ──────────────────────────────────────────────────
    override fun startup(): Long {
        Log.d(TAG, "Core startup")
        return 0L
    }

    override fun shutdown(): Long {
        Log.d(TAG, "Core requested shutdown")
        stopVpnTunnel()
        return 0L    }

    override fun onEmitStatus(status: Int, message: String?) {
        Log.d(TAG, "Core status [$status]: $message")
    }
}