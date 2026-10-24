package com.roohu.roohux.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.roohu.roohux.core.settings.ThemeMode

val Midnight = Color(0xFF0B1020)
val Violet = Color(0xFF7C5CFF)
val Cyan = Color(0xFF22D3EE)
val SoftPurple = Color(0xFFB69CFF)

private val DarkColors = darkColorScheme(
    primary = Violet, onPrimary = Color.White,
    secondary = Cyan, onSecondary = Color(0xFF00252B),
    tertiary = SoftPurple,
    background = Midnight, onBackground = Color(0xFFE8EAF6),
    surface = Color(0xFF0F1630), onSurface = Color(0xFFE8EAF6),
    surfaceVariant = Color(0xFF1A2342), onSurfaceVariant = Color(0xFFA9B1D6),
    error = Color(0xFFFF6B81),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B3FE0), onPrimary = Color.White,
    secondary = Color(0xFF0891B2),
    tertiary = Color(0xFF7A5BD6),
    background = Color(0xFFF6F7FB), onBackground = Color(0xFF12162B),
    surface = Color.White, onSurface = Color(0xFF12162B),
    surfaceVariant = Color(0xFFE7E9F5), onSurfaceVariant = Color(0xFF4A5070),
    error = Color(0xFFB3261E),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun RoohuXTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
