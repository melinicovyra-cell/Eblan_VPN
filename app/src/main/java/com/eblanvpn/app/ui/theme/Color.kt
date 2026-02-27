package com.eblanvpn.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Dark Background Palette ───────────────────────────────────────────────
val BackgroundDeep = Color(0xFF080C18)
val BackgroundMid  = Color(0xFF0D1221)
val SurfaceDark    = Color(0xFF111827)
val SurfaceCard    = Color(0xFF1A2035)
val SurfaceElevated = Color(0xFF1F2A40)

// ─── Accent Colors ─────────────────────────────────────────────────────────
val PurplePrimary  = Color(0xFF7C3AED)
val PurpleLight    = Color(0xFF9D5CF6)
val PurpleDim      = Color(0xFF4C1D95)
val CyanAccent     = Color(0xFF06B6D4)
val BlueAccent     = Color(0xFF2563EB)
val GreenAccent    = Color(0xFF10B981)
val OrangeAccent   = Color(0xFFEA580C)
val PinkAccent     = Color(0xFFDB2777)

// ─── Status Colors ─────────────────────────────────────────────────────────
val Connected     = Color(0xFF00D46A)
val ConnectedGlow = Color(0xFF00FF88)
val Connecting    = Color(0xFFFFAB40)
val Disconnected  = Color(0xFF546E7A)
val Error         = Color(0xFFFF5252)

// ─── Text ──────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFECEFF4)
val TextSecondary = Color(0xFF8892A4)
val TextHint      = Color(0xFF4A5568)

// ─── Gradient Definitions ──────────────────────────────────────────────────
val GradientPurpleCyan = listOf(PurplePrimary, CyanAccent)
val GradientConnected  = listOf(Color(0xFF00C853), Color(0xFF00E676))
val GradientConnecting = listOf(Color(0xFFFF8F00), Color(0xFFFFAB40))
val GradientError      = listOf(Color(0xFFD32F2F), Color(0xFFFF5252))
val GradientDisconnected = listOf(Color(0xFF37474F), Color(0xFF546E7A))
