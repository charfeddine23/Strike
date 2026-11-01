package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Cosmic Dark Palette
val CosmicDarkBg = Color(0xFF070B14)
val CosmicDarkSurface = Color(0xFF0F172A)
val CosmicDarkSurfaceElevated = Color(0xFF1E293B)
val GlassDarkPanel = Color(0x331E293B)
val GlassDarkBorder = Color(0x3338BDF8)
val GlassDarkHighlight = Color(0x1AFFFFFF)

// Cosmic Light Palette
val CosmicLightBg = Color(0xFFF1F5F9)
val CosmicLightSurface = Color(0xFFFFFFFF)
val CosmicLightSurfaceElevated = Color(0xFFE2E8F0)
val GlassLightPanel = Color(0xCCFFFFFF)
val GlassLightBorder = Color(0x4D0284C7)
val GlassLightHighlight = Color(0x66FFFFFF)

// Vivid Neon Accents for Liquid Glass
val NeonCyan = Color(0xFF00E5FF)
val NeonViolet = Color(0xFFA855F7)
val NeonEmerald = Color(0xFF10B981)
val NeonAmber = Color(0xFFF59E0B)
val NeonRose = Color(0xFFF43F5E)
val NeonSky = Color(0xFF38BDF8)
val NeonIndigo = Color(0xFF6366F1)

// Gradient Brushes
val CosmicGlowBrush = Brush.linearGradient(
    colors = listOf(NeonCyan, NeonViolet)
)

val StrikeFireBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFFF007A), Color(0xFFFF7A00))
)

val BoostEmeraldBrush = Brush.linearGradient(
    colors = listOf(NeonEmerald, NeonCyan)
)

val GlassBorderBrushDark = Brush.linearGradient(
    colors = listOf(
        Color(0x8038BDF8),
        Color(0x20A855F7),
        Color(0x6000E5FF)
    )
)

val GlassBorderBrushLight = Brush.linearGradient(
    colors = listOf(
        Color(0x600284C7),
        Color(0x309333EA),
        Color(0x5006B6D4)
    )
)
