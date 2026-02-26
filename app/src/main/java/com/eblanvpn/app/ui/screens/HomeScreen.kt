package com.eblanvpn.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.data.model.VpnState
import com.eblanvpn.app.ui.components.ConnectionButton
import com.eblanvpn.app.ui.components.TrafficPanel
import com.eblanvpn.app.ui.theme.*
import com.eblanvpn.app.utils.formatDuration

@Composable
fun HomeScreen(
    vpnState: VpnState,
    selectedServer: ServerConfig?,
    trafficStats: TrafficStats,
    connectionTime: Long,
    onToggleConnection: () -> Unit,
    onSelectServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to BackgroundMid,
                        0.5f to BackgroundDeep,
                        1f to BackgroundDeep
                    )
                )
            )
    ) {
        // Background decorative circles
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-80).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (vpnState == VpnState.CONNECTED)
                                Connected.copy(alpha = 0.06f)
                            else
                                PurplePrimary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "EBLAN VPN",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextSecondary
            )

            Spacer(Modifier.height(40.dp))

            // Connection Button
            ConnectionButton(
                state = vpnState,
                onClick = onToggleConnection
            )

            Spacer(Modifier.height(32.dp))

            // Connection Info
            ConnectionInfoCard(
                vpnState = vpnState,
                selectedServer = selectedServer,
                connectionTime = connectionTime,
                onSelectServer = onSelectServer
            )

            Spacer(Modifier.height(16.dp))

            // Traffic Stats
            TrafficPanel(
                stats = trafficStats,
                isVisible = vpnState == VpnState.CONNECTED,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConnectionInfoCard(
    vpnState: VpnState,
    selectedServer: ServerConfig?,
    connectionTime: Long,
    onSelectServer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status dot
                    val dotColor by androidx.compose.animation.animateColorAsState(
                        targetValue = when (vpnState) {
                            VpnState.CONNECTED -> Connected
                            VpnState.CONNECTING -> Connecting
                            VpnState.ERROR -> Error
                            else -> Disconnected
                        },
                        animationSpec = tween(300),
                        label = "dot_color"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(dotColor, shape = CircleShape)
                    )
                    Text(
                        text = when (vpnState) {
                            VpnState.CONNECTED -> "Подключено"
                            VpnState.CONNECTING -> "Подключение..."
                            VpnState.DISCONNECTING -> "Отключение..."
                            VpnState.ERROR -> "Ошибка"
                            VpnState.DISCONNECTED -> "Не подключено"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = when (vpnState) {
                            VpnState.CONNECTED -> Connected
                            VpnState.CONNECTING -> Connecting
                            VpnState.ERROR -> Error
                            else -> TextSecondary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Timer
                AnimatedVisibility(
                    visible = vpnState == VpnState.CONNECTED,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    Text(
                        text = formatDuration(connectionTime),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = SurfaceElevated)
            Spacer(Modifier.height(16.dp))

            // Server selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Сервер",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                    Spacer(Modifier.height(4.dp))
                    if (selectedServer != null) {
                        Text(
                            text = selectedServer.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${selectedServer.address}:${selectedServer.port}  •  ${selectedServer.protocolLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "Не выбран",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextHint
                        )
                    }
                }

                IconButton(onClick = onSelectServer) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Сменить сервер",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

