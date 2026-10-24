package com.roohu.roohux.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import com.roohu.roohux.core.settings.ThemeMode

val Midnight = Color(0xFF0B1020)
val Violet = Color(0xFF7C5CFF)
val Cyan = Color(0xFF22D3EE)
val SoftPurple = Color(0xFFFFC66D)
private val RoohuTypography = Typography(defaultFontFamily = FontFamily.SansSerif)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8CFF), onPrimary = Color(0xFF17102D),
    secondary = Color(0xFF67E8D5), onSecondary = Color(0xFF082A2A),
    tertiary = SoftPurple,
    background = Color(0xFF090D18), onBackground = Color(0xFFF4F1FF),
    surface = Color(0xFF111827), onSurface = Color(0xFFF4F1FF),
    surfaceVariant = Color(0xFF20283A), onSurfaceVariant = Color(0xFFB6C0D2),
    error = Color(0xFFFF718B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5944C6), onPrimary = Color.White,
    secondary = Color(0xFF087F8C), onSecondary = Color.White,
    tertiary = Color(0xFF9A6417),
    background = Color(0xFFF5F3EE), onBackground = Color(0xFF191A25),
    surface = Color(0xFFFFFEFA), onSurface = Color(0xFF191A25),
    surfaceVariant = Color(0xFFE9E5DD), onSurfaceVariant = Color(0xFF555968),
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
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = RoohuTypography, content = content)
}
