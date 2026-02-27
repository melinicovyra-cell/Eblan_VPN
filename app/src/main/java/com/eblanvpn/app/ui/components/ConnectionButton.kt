package com.eblanvpn.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eblanvpn.app.data.model.VpnState
import com.eblanvpn.app.ui.theme.*

@Composable
fun ConnectionButton(
    state: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "button_anim")

    // Pulse ring animation when connected
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Rotation for connecting state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Scale on press
    val pressScale by animateFloatAsState(
        targetValue = if (state == VpnState.CONNECTING) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    val buttonColors = when (state) {
        VpnState.CONNECTED -> GradientConnected
        VpnState.CONNECTING -> GradientConnecting
        VpnState.ERROR -> GradientError
        else -> listOf(Color(0xFF3D1A6B), Color(0xFF5B21B6))
    }

    val glowColor = when (state) {
        VpnState.CONNECTED -> Connected
        VpnState.CONNECTING -> Connecting
        VpnState.ERROR -> Error
        else -> PurplePrimary
    }

    val statusText = when (state) {
        VpnState.CONNECTED -> "Нажмите для отключения"
        VpnState.CONNECTING -> "Подключение..."
        VpnState.DISCONNECTING -> "Отключение..."
        VpnState.ERROR -> "Ошибка — повторить"
        VpnState.DISCONNECTED -> "Нажмите для подключения"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Outer glow ring (pulse when connected)
            if (state == VpnState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(glowColor.copy(alpha = 0.15f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(pulseScale * 0.9f)
                        .graphicsLayer { alpha = pulseAlpha * 0.8f }
                        .background(glowColor.copy(alpha = 0.12f), CircleShape)
                )
            }

            // Connecting ring
            if (state == VpnState.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(178.dp)
                        .graphicsLayer { rotationZ = rotation },
                    color = Connecting,
                    strokeWidth = 3.dp,
                    trackColor = Color.Transparent
                )
            }

            // Shadow/glow backdrop
            Box(
                modifier = Modifier
                    .size(158.dp)
                    .shadow(
                        elevation = if (state == VpnState.CONNECTED) 32.dp else 8.dp,
                        shape = CircleShape,
                        ambientColor = glowColor.copy(alpha = 0.5f),
                        spotColor = glowColor.copy(alpha = 0.5f)
                    )
            )

            // Main button circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(155.dp)
                    .scale(pressScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = buttonColors,
                            radius = 200f
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = state != VpnState.CONNECTING && state != VpnState.DISCONNECTING,
                        onClick = onClick
                    )
            ) {
                // Inner glow
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = "Toggle VPN",
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state == VpnState.CONNECTED) Connected
                    else TextSecondary,
            fontWeight = if (state == VpnState.CONNECTED) FontWeight.Medium else FontWeight.Normal
        )
    }
}
