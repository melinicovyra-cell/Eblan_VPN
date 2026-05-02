package com.eblanvpn.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eblanvpn.app.data.model.ServerConfig
import com.eblanvpn.app.ui.theme.*

private fun protocolColors(protocol: String): List<Color> = when (protocol.lowercase()) {
    "vless"       -> listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
    "vmess"       -> listOf(Color(0xFF0891B2), Color(0xFF0E7490))
    "trojan"      -> listOf(Color(0xFFEA580C), Color(0xFFC2410C))
    "shadowsocks" -> listOf(Color(0xFF059669), Color(0xFF047857))
    else          -> listOf(SurfaceElevated, SurfaceElevated)
}

private fun protocolAccent(protocol: String): Color = when (protocol.lowercase()) {
    "vless"       -> Color(0xFF7C3AED)
    "vmess"       -> Color(0xFF0891B2)
    "trojan"      -> Color(0xFFEA580C)
    "shadowsocks" -> Color(0xFF059669)
    else          -> SurfaceElevated
}

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

    val accent = protocolAccent(server.protocol)
    val active = isSelected || isConnected

    val tintAlpha by animateFloatAsState(
        targetValue = if (isConnected) 0.14f else if (isSelected) 0.10f else 0.04f,
        animationSpec = tween(300),
        label = "tint"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0.25f,
        animationSpec = tween(300),
        label = "border"
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (active) 1.5f else 0.8f,
        animationSpec = tween(300),
        label = "border_w"
    )

    val borderBrush = if (isConnected)
        Brush.horizontalGradient(GradientConnected)
    else
        Brush.horizontalGradient(listOf(accent.copy(alpha = borderAlpha), accent.copy(alpha = borderAlpha * 0.4f)))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = tintAlpha), SurfaceCard)
                )
            )
            .border(
                width = borderWidth.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        // Left accent bar — always visible, brighter when active
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .background(
                    Brush.verticalGradient(
                        if (isConnected) GradientConnected
                        else listOf(accent.copy(alpha = if (active) 1f else 0.35f), accent.copy(alpha = if (active) 0.6f else 0.15f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProtocolBadge(protocol = server.protocol)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
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
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    NetworkChip(server.network)
                    SecurityChip(server.security)
                    if (server.latency > 0) LatencyChip(server.latency)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Connected, CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(34.dp)
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
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = TextSecondary) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = Error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolBadge(protocol: String) {
    val colors = protocolColors(protocol)
    val label = when (protocol.lowercase()) {
        "vless"       -> "VL"
        "vmess"       -> "VM"
        "trojan"      -> "TJ"
        "shadowsocks" -> "SS"
        else          -> protocol.take(2).uppercase()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(colors))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun NetworkChip(network: String) {
    val (bg, fg) = when (network.lowercase()) {
        "ws"          -> Color(0xFF2563EB).copy(alpha = 0.15f) to Color(0xFF60A5FA)
        "grpc"        -> Color(0xFFEA580C).copy(alpha = 0.15f) to Color(0xFFFB923C)
        "h2"          -> Color(0xFF0891B2).copy(alpha = 0.15f) to Color(0xFF22D3EE)
        "httpupgrade" -> Color(0xFF7C3AED).copy(alpha = 0.15f) to Color(0xFFA78BFA)
        "splithttp"   -> Color(0xFF7C3AED).copy(alpha = 0.15f) to Color(0xFFA78BFA)
        else          -> SurfaceElevated to TextHint  // tcp
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = network.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SecurityChip(security: String) {
    val (bg, fg) = when (security.lowercase()) {
        "tls"     -> Color(0xFF059669).copy(alpha = 0.15f) to Color(0xFF34D399)
        "reality" -> Color(0xFF7C3AED).copy(alpha = 0.15f) to Color(0xFFC084FC)
        else      -> SurfaceElevated to TextHint
    }
    val label = when (security.lowercase()) {
        "tls"     -> "TLS"
        "reality" -> "Reality"
        else      -> "None"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LatencyChip(latency: Long) {
    val color = when {
        latency < 100 -> Color(0xFF10B981)
        latency < 300 -> Color(0xFFFFAB40)
        else          -> Color(0xFFFF5252)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "${latency}ms",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
