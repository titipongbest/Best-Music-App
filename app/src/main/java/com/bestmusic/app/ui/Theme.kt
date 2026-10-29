package com.bestmusic.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBg = Color(0xFF1B1B1D)
val AppSurface = Color(0xFF262628)
val AppSurfaceHigh = Color(0xFF303033)
val AppAccent = Color(0xFFF48FB1)
val TextSecondary = Color(0xFFA9A9AD)

private val scheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color(0xFF3A1020),
    secondary = Color(0xFFD9C3C4),
    background = AppBg,
    onBackground = Color.White,
    surface = AppBg,
    onSurface = Color.White,
    surfaceVariant = AppSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceHigh,
    surfaceContainerHighest = AppSurfaceHigh,
    outline = Color(0xFF4A4A4E),
)

@Composable
fun BestMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
