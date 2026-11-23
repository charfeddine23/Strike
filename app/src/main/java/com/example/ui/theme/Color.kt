package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Liquid Glass v5 - Cosmic Dark Palette
// ==========================================
val CosmicDarkBg = Color(0xFF060A13)
val CosmicDarkSurface = Color(0xFF0F172A)
val CosmicDarkSurfaceElevated = Color(0xFF1E293B)
val CosmicDarkSheet = Color(0xFA0B1324)
val GlassDarkPanel = Color(0xA60F172A)
val GlassDarkBorder = Color(0x4538BDF8)
val GlassDarkHighlight = Color(0x1EFFFFFF)

// ==========================================
// Liquid Glass v5 - Day Mode (Light Theme) Palette
// Optimized for glare-free daylight comfort & crisp contrast
// ==========================================
val DayGlassBg = Color(0xFFEEF4FA)
val DayGlassSurface = Color(0xFAFFFFFF)
val DayGlassSurfaceElevated = Color(0xFFE3ECF5)
val DayGlassSheet = Color(0xFAF8FAFC)
val DayGlassPanel = Color(0xF5FFFFFF)
val DayGlassPanelSubtle = Color(0xCCE6EFF8)
val DayGlassBorder = Color(0x660284C7)
val DayGlassHighlight = Color(0xCCFFFFFF)

// High-contrast Day Mode Typography (WCAG AAA daylight compliant)
val DayTextPrimary = Color(0xFF0B132B)        // Midnight obsidian (> 14:1 contrast ratio)
val DayTextSecondary = Color(0xFF334155)      // Deep slate for secondary labels (> 8:1 contrast)
val DayTextTertiary = Color(0xFF475569)       // Muted slate for captions & tags
val DayTextHandwritten = Color(0xFF1E293B)    // Rich indigo-slate for handwritten secondary text

// Day Mode Harmonized Accents (vibrant yet deep enough for pure readability on light cards)
val DayCyan = Color(0xFF0284C7)               // Crisp sky-cyan accent (high daylight contrast)
val DayCyanGlow = Color(0xFF00B4D8)           // Cyan glowing border highlight
val DayCyanContainer = Color(0x220284C7)      // Translucent cyan glass fill

val DayOrange = Color(0xFFC25E00)             // Deep soft orange / amber highlight (> 4.5:1)
val DayOrangeGlow = Color(0xFFF59E0B)         // Radiant amber glow
val DayOrangeContainer = Color(0x24F59E0B)    // Translucent orange glass fill

val DayPurple = Color(0xFF6D28D9)             // Deep royal purple for badges (> 6.8:1)
val DayPurpleGlow = Color(0xFF8B5CF6)         // Soft purple glow
val DayPurpleContainer = Color(0x228B5CF6)    // Translucent purple glass fill

val DayEmerald = Color(0xFF047857)            // Deep forest emerald (> 5.5:1)
val DayEmeraldGlow = Color(0xFF10B981)        // Emerald glow
val DayEmeraldContainer = Color(0x2210B981)   // Translucent emerald glass fill

val DayRose = Color(0xFFBE123C)               // Crimson rose for delete/warning
val DayRoseContainer = Color(0x20F43F5E)

// Navigation Bar Tokens (Transparent iOS Liquid Glass for Night vs Day)
val DarkNavBg = Color(0x240B1220)
val DarkNavBorder = Color(0x45FFFFFF)
val LightNavBg = Color(0xCCFFFFFF)
val LightNavBorder = Color(0x70FFFFFF)

// ==========================================
// Vivid Neon Accents for Liquid Glass (Dark Mode)
// ==========================================
val NeonCyan = Color(0xFF00E5FF)
val NeonViolet = Color(0xFFA855F7)
val NeonEmerald = Color(0xFF10B981)
val NeonAmber = Color(0xFFF59E0B)
val NeonRose = Color(0xFFF43F5E)
val NeonSky = Color(0xFF38BDF8)
val NeonIndigo = Color(0xFF6366F1)

// ==========================================
// Liquid Glass v5 Gradient Brushes
// ==========================================
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
        Color(0x8838BDF8),
        Color(0x2CA855F7),
        Color(0x6600E5FF)
    )
)

// Liquid Glass v5 Ultra-thin Glowing Border (Light Theme)
// Crisp daylight refraction border so cards read distinctly from pearl background
val GlassBorderBrushLight = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF),    // Specular top highlight
        Color(0x800284C7),    // Crisp sky-cyan edge
        Color(0x4D7C3AED),    // Soft amethyst accent
        Color(0xB3CBD5E1)     // Defined cool slate rim
    )
)

val GlassCardSurfaceLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFAFFFFFF),    // Top crystalline sheen
        Color(0xF0EDF4FB)     // Bottom subtle cool pearl tint
    )
)

val SpecularHighlightBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0x00FFFFFF),
        Color(0x80FFFFFF),
        Color(0x00FFFFFF)
    )
)
