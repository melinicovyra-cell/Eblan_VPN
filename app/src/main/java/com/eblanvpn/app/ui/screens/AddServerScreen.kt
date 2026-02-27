package com.eblanvpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    initialConfig: ServerConfig? = null,
    onSave: (ServerConfig) -> Unit,
    onCancel: () -> Unit,
    onPasteLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEdit = initialConfig != null

    // Form state
    var name by remember { mutableStateOf(initialConfig?.name ?: "") }
    var address by remember { mutableStateOf(initialConfig?.address ?: "") }
    var port by remember { mutableStateOf(initialConfig?.port?.toString() ?: "443") }
    var uuid by remember { mutableStateOf(initialConfig?.uuid ?: "") }
    var protocol by remember { mutableStateOf(initialConfig?.protocol ?: "vless") }
    var network by remember { mutableStateOf(initialConfig?.network ?: "tcp") }
    var security by remember { mutableStateOf(initialConfig?.security ?: "none") }
    var sni by remember { mutableStateOf(initialConfig?.sni ?: "") }
    var fingerprint by remember { mutableStateOf(initialConfig?.fingerprint ?: "chrome") }
    var flow by remember { mutableStateOf(initialConfig?.flow ?: "") }
    var publicKey by remember { mutableStateOf(initialConfig?.publicKey ?: "") }
    var shortId by remember { mutableStateOf(initialConfig?.shortId ?: "") }
    var wsPath by remember { mutableStateOf(initialConfig?.wsPath ?: "/") }
    var wsHost by remember { mutableStateOf(initialConfig?.wsHost ?: "") }
    var grpcService by remember { mutableStateOf(initialConfig?.grpcServiceName ?: "") }
    var allowInsecure by remember { mutableStateOf(initialConfig?.allowInsecure ?: false) }

    var protocolMenuExpanded by remember { mutableStateOf(false) }
    var networkMenuExpanded by remember { mutableStateOf(false) }
    var securityMenuExpanded by remember { mutableStateOf(false) }

    val protocols = listOf("vless", "vmess", "trojan", "shadowsocks")
    val networks = listOf("tcp", "ws", "grpc", "h2", "httpupgrade", "splithttp")
    val securities = listOf("none", "tls", "reality")

    fun buildConfig(): ServerConfig {
        return (initialConfig ?: ServerConfig()).copy(
            name = name.ifEmpty { "${address}:${port}" },
            address = address,
            port = port.toIntOrNull() ?: 443,
            uuid = uuid,
            protocol = protocol,
            network = network,
            security = security,
            sni = sni,
            fingerprint = fingerprint,
            flow = flow,
            publicKey = publicKey,
            shortId = shortId,
            wsPath = wsPath,
            wsHost = wsHost,
            grpcServiceName = grpcService,
            allowInsecure = allowInsecure
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    if (isEdit) "Изменить сервер" else "Добавить сервер",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, "Отмена", tint = TextSecondary)
                }
            },
            actions = {
                TextButton(
                    onClick = { if (address.isNotBlank() && uuid.isNotBlank()) onSave(buildConfig()) },
                    colors = ButtonDefaults.textButtonColors(contentColor = PurplePrimary)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDeep)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Paste link button
            Button(
                onClick = onPasteLink,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary.copy(alpha = 0.15f),
                    contentColor = PurpleLight
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.ContentPaste, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Вставить ссылку (vless/vmess/trojan/ss)")
            }

            SectionHeader("Основное")

            VpnTextField(label = "Название (необязательно)", value = name, onValue = { name = it })

            // Protocol selector
            ExposedDropdownMenuBox(
                expanded = protocolMenuExpanded,
                onExpandedChange = { protocolMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = protocol.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Протокол") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(protocolMenuExpanded) },
                    colors = vpnTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = protocolMenuExpanded,
                    onDismissRequest = { protocolMenuExpanded = false },
                    modifier = Modifier.background(SurfaceElevated)
                ) {
                    protocols.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.uppercase(), color = TextPrimary) },
                            onClick = { protocol = p; protocolMenuExpanded = false }
                        )
                    }
                }
            }

            SectionHeader("Подключение")

            VpnTextField(
                label = "Адрес",
                value = address,
                onValue = { address = it },
                placeholder = "example.com или IP"
            )
            VpnTextField(
                label = "Порт",
                value = port,
                onValue = { port = it },
                keyboardType = KeyboardType.Number
            )

            SectionHeader("Аутентификация")

            VpnTextField(
                label = when (protocol) {
                    "trojan" -> "Пароль"
                    "shadowsocks" -> "Пароль"
                    else -> "UUID"
                },
                value = uuid,
                onValue = { uuid = it }
            )

            if (protocol == "vless") {
                VpnTextField(
                    label = "Flow (необязательно)",
                    value = flow,
                    onValue = { flow = it },
                    placeholder = "xtls-rprx-vision"
                )
            }

            SectionHeader("Транспорт")

            // Network
            ExposedDropdownMenuBox(
                expanded = networkMenuExpanded,
                onExpandedChange = { networkMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = network,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Сеть") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(networkMenuExpanded) },
                    colors = vpnTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = networkMenuExpanded,
                    onDismissRequest = { networkMenuExpanded = false },
                    modifier = Modifier.background(SurfaceElevated)
                ) {
                    networks.forEach { n ->
                        DropdownMenuItem(
                            text = { Text(n, color = TextPrimary) },
                            onClick = { network = n; networkMenuExpanded = false }
                        )
                    }
                }
            }

            when (network) {
                "ws" -> {
                    VpnTextField("WebSocket Path", wsPath, { wsPath = it }, placeholder = "/")
                    VpnTextField("WebSocket Host", wsHost, { wsHost = it })
                }
                "grpc" -> {
                    VpnTextField("gRPC Service Name", grpcService, { grpcService = it })
                }
            }

            SectionHeader("Безопасность (TLS/Reality)")

            // Security
            ExposedDropdownMenuBox(
                expanded = securityMenuExpanded,
                onExpandedChange = { securityMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = security,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Безопасность") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(securityMenuExpanded) },
                    colors = vpnTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = securityMenuExpanded,
                    onDismissRequest = { securityMenuExpanded = false },
                    modifier = Modifier.background(SurfaceElevated)
                ) {
                    securities.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s, color = TextPrimary) },
                            onClick = { security = s; securityMenuExpanded = false }
                        )
                    }
                }
            }

            if (security == "tls" || security == "reality") {
                VpnTextField("SNI", sni, { sni = it }, placeholder = "example.com")
                VpnTextField("Fingerprint", fingerprint, { fingerprint = it }, placeholder = "chrome")
            }

            if (security == "reality") {
                VpnTextField("Public Key", publicKey, { publicKey = it })
                VpnTextField("Short ID", shortId, { shortId = it })
            }

            if (security == "tls") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Разрешить небезопасное подключение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = allowInsecure,
                        onCheckedChange = { allowInsecure = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { if (address.isNotBlank() && uuid.isNotBlank()) onSave(buildConfig()) },
                enabled = address.isNotBlank() && uuid.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    if (isEdit) Icons.Rounded.Save else Icons.Rounded.Add,
                    null,
                    Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEdit) "Сохранить изменения" else "Добавить сервер",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = PurpleLight,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun VpnTextField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = TextHint) }
        } else null,
        colors = vpnTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun vpnTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PurplePrimary,
    unfocusedBorderColor = SurfaceElevated,
    focusedLabelColor = PurpleLight,
    unfocusedLabelColor = TextHint,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PurplePrimary,
    focusedContainerColor = SurfaceCard,
    unfocusedContainerColor = SurfaceCard
)

