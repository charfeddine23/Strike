package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF03141F),
    primaryContainer = Color(0xFF0C2D48),
    onPrimaryContainer = Color(0xFFB8E6FF),
    secondary = NeonViolet,
    onSecondary = Color(0xFF2E0854),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = NeonEmerald,
    onTertiary = Color(0xFF022C22),
    background = CosmicDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = CosmicDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CosmicDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

// Liquid Glass v5 Day Mode Color Scheme
// Designed for pure daylight clarity, eye comfort, and high contrast ratios
private val LightColorScheme = lightColorScheme(
    primary = DayCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D61),
    secondary = DayPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = DayEmerald,
    onTertiary = Color.White,
    background = DayGlassBg,
    onBackground = DayTextPrimary,
    surface = DayGlassSurface,
    onSurface = DayTextPrimary,
    surfaceVariant = DayGlassSurfaceElevated,
    onSurfaceVariant = DayTextSecondary,
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun StrikePulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
