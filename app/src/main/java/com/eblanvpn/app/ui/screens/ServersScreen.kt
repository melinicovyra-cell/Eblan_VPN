package com.eblanvpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.VpnState
import com.eblanvpn.app.ui.components.ServerCard
import com.eblanvpn.app.ui.theme.*

@Composable
fun ServersScreen(
    servers: List<ServerConfig>,
    selectedServer: ServerConfig?,
    vpnState: VpnState,
    onSelectServer: (ServerConfig) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (ServerConfig) -> Unit,
    onDeleteServer: (ServerConfig) -> Unit,
    onImportFromClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            ServersTopBar(
                serverCount = servers.size,
                onImportClipboard = onImportFromClipboard,
                onAddServer = onAddServer
            )

            if (servers.isEmpty()) {
                EmptyServersState(
                    onAddServer = onAddServer,
                    onImportClipboard = onImportFromClipboard,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(servers, key = { it.id }) { server ->
                        val isSelected = server.id == selectedServer?.id
                        val isConnected = isSelected && vpnState == VpnState.CONNECTED

                        ServerCard(
                            server = server,
                            isSelected = isSelected,
                            isConnected = isConnected,
                            onSelect = { onSelectServer(server) },
                            onEdit = { onEditServer(server) },
                            onDelete = { onDeleteServer(server) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddServer,
            containerColor = PurplePrimary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Добавить сервер")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServersTopBar(
    serverCount: Int,
    onImportClipboard: () -> Unit,
    onAddServer: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Серверы",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (serverCount > 0) {
                    Text(
                        text = "$serverCount конфигураций",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onImportClipboard) {
                Icon(
                    Icons.Rounded.ContentPaste,
                    contentDescription = "Импорт из буфера",
                    tint = TextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundDeep,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
private fun EmptyServersState(
    onAddServer: () -> Unit,
    onImportClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Cloud,
            contentDescription = null,
            tint = TextHint,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Нет серверов",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Добавьте сервер вручную или\nвставьте конфигурацию из буфера обмена",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddServer,
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Добавить сервер")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onImportClipboard,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Импорт из буфера обмена")
        }
    }
}
