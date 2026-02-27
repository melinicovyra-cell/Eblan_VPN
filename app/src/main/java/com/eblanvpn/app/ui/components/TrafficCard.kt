package com.eblanvpn.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblanvpn.app.data.model.TrafficStats
import com.eblanvpn.app.ui.theme.*

@Composable
fun TrafficPanel(
    stats: TrafficStats,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrafficCard(
                icon = Icons.Rounded.ArrowUpward,
                label = "Исходящий",
                speed = TrafficStats.formatSpeed(stats.uploadSpeed),
                total = TrafficStats.formatBytes(stats.totalUpload),
                gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                iconTint = Color(0xFFB794F4),
                modifier = Modifier.weight(1f)
            )
            TrafficCard(
                icon = Icons.Rounded.ArrowDownward,
                label = "Входящий",
                speed = TrafficStats.formatSpeed(stats.downloadSpeed),
                total = TrafficStats.formatBytes(stats.totalDownload),
                gradientColors = listOf(Color(0xFF0891B2), Color(0xFF0E7490)),
                iconTint = Color(0xFF67E8F9),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrafficCard(
    icon: ImageVector,
    label: String,
    speed: String,
    total: String,
    gradientColors: List<Color>,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SurfaceElevated, SurfaceCard)
                )
            )
    ) {
        // Subtle colored border on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Brush.horizontalGradient(gradientColors))
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon + label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Speed (big number)
            Text(
                text = speed,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )

            // Total
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Всего:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
                Text(
                    text = total,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
