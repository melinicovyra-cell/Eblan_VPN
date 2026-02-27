package com.eblanvpn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.ui.theme.*

@Composable
fun ServerCard(
    server: ServerConfig,
    isSelected: Boolean,
    isConnected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isConnected -> Connected
            isSelected -> PurplePrimary
            else -> Color.Transparent
        },
        label = "border_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(
                width = if (isSelected || isConnected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        // Left accent bar
        if (isSelected || isConnected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isConnected) GradientConnected else GradientPurpleCyan
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Protocol badge
            ProtocolBadge(protocol = server.protocol, isSelected = isSelected || isConnected)

            // Server info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) TextPrimary else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(server.network.uppercase())
                    Chip(server.securityLabel)
                    if (server.latency > 0) {
                        LatencyChip(server.latency)
                    }
                }
            }

            // Status indicator / menu
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Connected, CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Меню",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Изменить", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Edit, null, tint = TextSecondary)
                            },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = Error) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Delete, null, tint = Error)
                            },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolBadge(protocol: String, isSelected: Boolean) {
    val colors = when (protocol.lowercase()) {
        "vless" -> listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
        "vmess" -> listOf(Color(0xFF0891B2), Color(0xFF0E7490))
        "trojan" -> listOf(Color(0xFFEA580C), Color(0xFFC2410C))
        "shadowsocks" -> listOf(Color(0xFF059669), Color(0xFF047857))
        else -> listOf(SurfaceElevated, SurfaceElevated)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(colors))
    ) {
        Text(
            text = when (protocol.lowercase()) {
                "vless" -> "VL"
                "vmess" -> "VM"
                "trojan" -> "TJ"
                "shadowsocks" -> "SS"
                else -> protocol.take(2).uppercase()
            },
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextHint
        )
    }
}

@Composable
private fun LatencyChip(latency: Long) {
    val color = when {
        latency < 100 -> Color(0xFF10B981)
        latency < 300 -> Color(0xFFFFAB40)
        else -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${latency}ms",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
