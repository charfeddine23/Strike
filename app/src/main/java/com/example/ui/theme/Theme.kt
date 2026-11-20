package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val prefValue: String) {
    SYSTEM("system"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromPref(value: String?): AppThemeMode {
            return entries.firstOrNull { it.prefValue == value } ?: SYSTEM
        }
    }
}

val LocalAppDarkTheme = staticCompositionLocalOf { true }

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
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = NeonRose,
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6),
    background = CosmicDarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = CosmicDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CosmicDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

// Liquid Glass v5 Day Mode Color Scheme
// Designed for pure daylight clarity, eye comfort, and zero color overlap with Dark Mode
private val LightColorScheme = lightColorScheme(
    primary = DayCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = DayPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = DayEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    error = DayRose,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),
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

    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
