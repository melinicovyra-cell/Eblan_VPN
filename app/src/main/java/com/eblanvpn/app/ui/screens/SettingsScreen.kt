package com.eblanvpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.BuildConfig
import com.eblanvpn.app.data.model.AccentColor
import com.eblanvpn.app.data.model.AppSettings
import com.eblanvpn.app.data.model.RoutingMode
import com.eblanvpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAccentColor: (AccentColor) -> Unit,
    onDarkTheme: (Boolean) -> Unit,
    onDns1: (String) -> Unit,
    onDns2: (String) -> Unit,
    onDnsOverTls: (Boolean) -> Unit,
    onBypassLan: (Boolean) -> Unit,
    onIpv6: (Boolean) -> Unit,
    onRoutingMode: (RoutingMode) -> Unit,
    onAutoConnect: (Boolean) -> Unit,
    onShowNotificationTraffic: (Boolean) -> Unit,
    onMtu: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Настройки",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ─── Appearance ───────────────────────────────────────────────────
            item { SettingsSection("Внешний вид") }

            item {
                // Accent color picker
                SettingCard(
                    icon = Icons.Rounded.Palette,
                    title = "Цвет акцента",
                    subtitle = settings.accentColor.label
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentColor.entries.forEach { color ->
                            val isSelected = settings.accentColor == color
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color.colorHex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { onAccentColor(color) }
                            )
                        }
                    }
                }
            }

            item {
                SettingToggle(
                    icon = Icons.Rounded.DarkMode,
                    title = "Тёмная тема",
                    subtitle = "Всегда использовать тёмную тему",
                    checked = settings.useDarkTheme,
                    onChecked = onDarkTheme
                )
            }

            // ─── Connection ───────────────────────────────────────────────────
            item { SettingsSection("Подключение") }

            item {
                var showRoutingMenu by remember { mutableStateOf(false) }
                SettingCard(
                    icon = Icons.Rounded.Route,
                    title = "Режим маршрутизации",
                    subtitle = settings.routingMode.label,
                    onClick = { showRoutingMenu = true }
                ) {
                    DropdownMenu(
                        expanded = showRoutingMenu,
                        onDismissRequest = { showRoutingMenu = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        RoutingMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label, color = TextPrimary) },
                                onClick = { onRoutingMode(mode); showRoutingMenu = false },
                                leadingIcon = {
                                    if (settings.routingMode == mode) {
                                        Icon(Icons.Rounded.Check, null, tint = PurplePrimary)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingToggle(
                    icon = Icons.Rounded.LanOutlined,
                    title = "Обход локальной сети",
                    subtitle = "LAN трафик не идёт через VPN",
                    checked = settings.enableBypassLan,
                    onChecked = onBypassLan
                )
            }

            item {
                SettingToggle(
                    icon = Icons.Rounded.NetworkPing,
                    title = "IPv6",
                    subtitle = "Маршрутизировать IPv6 трафик",
                    checked = settings.enableIpv6,
                    onChecked = onIpv6
                )
            }

            item {
                SettingToggle(
                    icon = Icons.Rounded.Link,
                    title = "Автоподключение",
                    subtitle = "Подключаться при запуске",
                    checked = settings.autoConnect,
                    onChecked = onAutoConnect
                )
            }

            // ─── DNS ──────────────────────────────────────────────────────────
            item { SettingsSection("DNS") }

            item {
                SettingToggle(
                    icon = Icons.Rounded.Lock,
                    title = "DNS over TLS",
                    subtitle = "Шифровать DNS запросы",
                    checked = settings.enableDnsOverTls,
                    onChecked = onDnsOverTls
                )
            }

            item {
                var editDns1 by remember { mutableStateOf(false) }
                var dns1Text by remember(settings.dns1) { mutableStateOf(settings.dns1) }
                SettingCard(
                    icon = Icons.Rounded.Dns,
                    title = "Основной DNS",
                    subtitle = settings.dns1,
                    onClick = { editDns1 = true }
                )
                if (editDns1) {
                    AlertDialog(
                        onDismissRequest = { editDns1 = false },
                        containerColor = SurfaceCard,
                        title = { Text("Основной DNS", color = TextPrimary) },
                        text = {
                            OutlinedTextField(
                                value = dns1Text,
                                onValueChange = { dns1Text = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = SurfaceElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = PurplePrimary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { onDns1(dns1Text); editDns1 = false }) {
                                Text("OK", color = PurplePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editDns1 = false }) {
                                Text("Отмена", color = TextSecondary)
                            }
                        }
                    )
                }
            }

            item {
                var editDns2 by remember { mutableStateOf(false) }
                var dns2Text by remember(settings.dns2) { mutableStateOf(settings.dns2) }
                SettingCard(
                    icon = Icons.Rounded.Dns,
                    title = "Резервный DNS",
                    subtitle = settings.dns2,
                    onClick = { editDns2 = true }
                )
                if (editDns2) {
                    AlertDialog(
                        onDismissRequest = { editDns2 = false },
                        containerColor = SurfaceCard,
                        title = { Text("Резервный DNS", color = TextPrimary) },
                        text = {
                            OutlinedTextField(
                                value = dns2Text,
                                onValueChange = { dns2Text = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = SurfaceElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = PurplePrimary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { onDns2(dns2Text); editDns2 = false }) {
                                Text("OK", color = PurplePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editDns2 = false }) {
                                Text("Отмена", color = TextSecondary)
                            }
                        }
                    )
                }
            }

            // ─── Notifications ────────────────────────────────────────────────
            item { SettingsSection("Уведомления") }

            item {
                SettingToggle(
                    icon = Icons.Rounded.Notifications,
                    title = "Трафик в уведомлении",
                    subtitle = "Показывать скорость в статичном уведомлении",
                    checked = settings.showNotificationTraffic,
                    onChecked = onShowNotificationTraffic
                )
            }

            // ─── Advanced ─────────────────────────────────────────────────────
            item { SettingsSection("Дополнительно") }

            item {
                var showMtuDialog by remember { mutableStateOf(false) }
                var mtuText by remember(settings.mtu) { mutableStateOf(settings.mtu.toString()) }
                SettingCard(
                    icon = Icons.Rounded.Tune,
                    title = "MTU",
                    subtitle = "${settings.mtu}",
                    onClick = { showMtuDialog = true }
                )
                if (showMtuDialog) {
                    AlertDialog(
                        onDismissRequest = { showMtuDialog = false },
                        containerColor = SurfaceCard,
                        title = { Text("MTU", color = TextPrimary) },
                        text = {
                            OutlinedTextField(
                                value = mtuText,
                                onValueChange = { mtuText = it },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = SurfaceElevated,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = PurplePrimary
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                mtuText.toIntOrNull()?.let { onMtu(it) }
                                showMtuDialog = false
                            }) {
                                Text("OK", color = PurplePrimary)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMtuDialog = false }) {
                                Text("Отмена", color = TextSecondary)
                            }
                        }
                    )
                }
            }

            // ─── About ────────────────────────────────────────────────────────
            item { SettingsSection("О приложении") }

            item {
                SettingCard(
                    icon = Icons.Rounded.Info,
                    title = "Версия",
                    subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                )
            }

            item {
                SettingCard(
                    icon = Icons.Rounded.Code,
                    title = "Ядро",
                    subtitle = "xray-core via libv2ray"
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = PurpleLight,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun SettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PurpleLight, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = TextHint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .clickable { onChecked(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PurpleLight, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PurplePrimary,
                uncheckedThumbColor = TextHint,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}
