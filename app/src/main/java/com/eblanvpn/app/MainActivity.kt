package com.eblanvpn.app

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.eblanvpn.app.ui.navigation.AppNavigation
import com.eblanvpn.app.ui.theme.EblanVPNTheme
import com.eblanvpn.app.utils.VlessParser
import com.eblanvpn.app.utils.getClipboardText
import com.eblanvpn.app.viewmodel.MainViewModel
import com.eblanvpn.app.viewmodel.SettingsViewModel
import com.eblanvpn.app.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app by lazy { application as App }

    private val mainViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(app, app.repository))
            .get(MainViewModel::class.java)
    }

    private val settingsViewModel by lazy {
        ViewModelProvider(this, ViewModelFactory(app, app.repository))
            .get(SettingsViewModel::class.java)
    }

    // VPN permission launcher
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            mainViewModel.onVpnPermissionGranted()
        } else {
            mainViewModel.onVpnPermissionDenied()
        }
    }

    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Handle intent (deep link / share link)
        handleIntent(intent)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            // Collect snackbar messages
            LaunchedEffect(Unit) {
                mainViewModel.snackbarMessage.collectLatest { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            EblanVPNTheme(accentColor = settings.accentColor) {
                AppNavigation(
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    onToggleConnection = {
                        mainViewModel.toggleConnection { permIntent ->
                            vpnPermissionLauncher.launch(permIntent)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val data = intent.data ?: return
        val uri = data.toString()

        if (uri.startsWith("vless://") || uri.startsWith("vmess://") ||
            uri.startsWith("trojan://") || uri.startsWith("ss://")) {
            val config = VlessParser.parse(uri)
            if (config != null) {
                mainViewModel.addServer(config)
                Toast.makeText(this, "Добавлен: ${config.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importFromClipboard() {
        val text = getClipboardText()
        if (text.isNullOrBlank()) {
            Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
            return
        }
        mainViewModel.importFromClipboard(text)
    }
}
