package com.eblanvpn.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.ui.screens.*
import com.eblanvpn.app.ui.theme.*
import com.eblanvpn.app.viewmodel.MainViewModel
import com.eblanvpn.app.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home    : Screen("home", Icons.Rounded.Shield, "Главная")
    object Servers : Screen("servers", Icons.Rounded.Storage, "Серверы")
    object Settings: Screen("settings", Icons.Rounded.Settings, "Настройки")
}

private val bottomNavItems = listOf(Screen.Home, Screen.Servers, Screen.Settings)

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    onToggleConnection: () -> Unit
) {
    val navController = rememberNavController()
    val currentBackstack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackstack?.destination?.route
    val clipboard = LocalClipboardManager.current

    val vpnState by mainViewModel.vpnState.collectAsState()
    val trafficStats by mainViewModel.trafficStats.collectAsState()
    val connectionTime by mainViewModel.connectionTime.collectAsState()
    val servers by mainViewModel.servers.collectAsState()
    val selectedServer by mainViewModel.selectedServer.collectAsState()
    val subscriptions by mainViewModel.subscriptions.collectAsState()
    val refreshingSubs by mainViewModel.refreshingSubs.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    var editingServer by remember { mutableStateOf<ServerConfig?>(null) }

    fun pasteFromClipboardSingle(): Boolean {
        val text = clipboard.getText()?.text?.trim().orEmpty()
        if (text.isEmpty()) {
            mainViewModel.emitSnackbar("Буфер обмена пуст")
            return false
        }
        val ok = mainViewModel.importSingleLink(text)
        if (!ok) mainViewModel.emitSnackbar("Не удалось распарсить ссылку")
        return ok
    }

    fun pasteFromClipboardBulk() {
        val text = clipboard.getText()?.text.orEmpty()
        if (text.isBlank()) {
            mainViewModel.emitSnackbar("Буфер обмена пуст")
            return
        }
        mainViewModel.importFromClipboard(text)
    }

    Scaffold(
        containerColor = BackgroundDeep,
        bottomBar = {
            if (currentRoute == Screen.Home.route ||
                currentRoute == Screen.Servers.route ||
                currentRoute == Screen.Settings.route) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    screen.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PurplePrimary,
                                selectedTextColor = PurplePrimary,
                                indicatorColor = PurplePrimary.copy(alpha = 0.12f),
                                unselectedIconColor = TextHint,
                                unselectedTextColor = TextHint
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    vpnState = vpnState,
                    selectedServer = selectedServer,
                    trafficStats = trafficStats,
                    connectionTime = connectionTime,
                    onToggleConnection = onToggleConnection,
                    onSelectServer = {
                        navController.navigate(Screen.Servers.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Servers.route) {
                ServersScreen(
                    servers = servers,
                    selectedServer = selectedServer,
                    vpnState = vpnState,
                    onSelectServer = { server ->
                        mainViewModel.selectServer(server)
                        navController.navigate(Screen.Home.route) {
                            launchSingleTop = true
                        }
                    },
                    onAddServer = {
                        editingServer = null
                        navController.navigate("add_server")
                    },
                    onEditServer = { server ->
                        editingServer = server
                        navController.navigate("edit_server")
                    },
                    onDeleteServer = { mainViewModel.deleteServer(it) },
                    onImportFromClipboard = { pasteFromClipboardBulk() },
                    onOpenSubscriptions = { navController.navigate("subscriptions") }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onAccentColor = settingsViewModel::setAccentColor,
                    onDarkTheme = settingsViewModel::setDarkTheme,
                    onDns1 = settingsViewModel::setDns1,
                    onDns2 = settingsViewModel::setDns2,
                    onDnsOverTls = settingsViewModel::setDnsOverTls,
                    onBypassLan = settingsViewModel::setBypassLan,
                    onIpv6 = settingsViewModel::setEnableIpv6,
                    onRoutingMode = settingsViewModel::setRoutingMode,
                    onAutoConnect = settingsViewModel::setAutoConnect,
                    onShowNotificationTraffic = settingsViewModel::setShowNotificationTraffic,
                    onMtu = settingsViewModel::setMtu,
                    onOpenAppPicker = { navController.navigate("app_picker") }
                )
            }

            composable("add_server") {
                AddServerScreen(
                    initialConfig = null,
                    onSave = { config ->
                        mainViewModel.addServer(config)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    onPasteLink = {
                        if (pasteFromClipboardSingle()) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable("edit_server") {
                val server = editingServer
                if (server != null) {
                    AddServerScreen(
                        initialConfig = server,
                        onSave = { config ->
                            mainViewModel.updateServer(config)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                        onPasteLink = {
                            if (pasteFromClipboardSingle()) {
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }

            composable("app_picker") {
                AppPickerScreen(
                    settings = settings,
                    onPerAppMode = settingsViewModel::setPerAppMode,
                    onTogglePackage = settingsViewModel::togglePerAppPackage,
                    onClearAll = { settingsViewModel.setPerAppList(emptySet()) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("subscriptions") {
                SubscriptionsScreen(
                    subscriptions = subscriptions,
                    refreshingIds = refreshingSubs,
                    onAdd = { name, url -> mainViewModel.addSubscription(name, url) },
                    onRefresh = { mainViewModel.refreshSubscription(it) },
                    onRefreshAll = { mainViewModel.refreshAllSubscriptions() },
                    onDelete = { mainViewModel.deleteSubscription(it) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
