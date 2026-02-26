package com.eblanvpn.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.eblanvpn.app.data.model.AccentColor

val LocalAccentColor = staticCompositionLocalOf { AccentColor.PURPLE }

private fun buildColorScheme(accent: AccentColor): ColorScheme {
    val primary = Color(accent.colorHex)
    val primaryContainer = primary.copy(alpha = 0.2f)
    val onPrimary = Color.White
    val secondary = CyanAccent

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary.copy(alpha = 0.9f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.15f),
        onSecondaryContainer = secondary,
        tertiary = Color(0xFF00D46A),
        onTertiary = Color.White,
        background = BackgroundDeep,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceCard,
        onSurfaceVariant = TextSecondary,
        surfaceTint = primary.copy(alpha = 0.05f),
        outline = Color(0xFF2D3748),
        outlineVariant = Color(0xFF1A2035),
        error = Error,
        onError = Color.White,
        inverseSurface = TextPrimary,
        inverseOnSurface = BackgroundDeep,
        scrim = Color.Black.copy(alpha = 0.6f)
    )
}

@Composable
fun EblanVPNTheme(
    accentColor: AccentColor = AccentColor.PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = buildColorScheme(accentColor)

    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
