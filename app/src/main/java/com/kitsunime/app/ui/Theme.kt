package com.kitsunime.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KitsunimeYellow = Color(0xFFFFD60A)

private val Colors = darkColorScheme(
    primary = KitsunimeYellow,
    onPrimary = Color(0xFF1A1500),
    primaryContainer = Color(0xFF3D3200),
    onPrimaryContainer = Color(0xFFFFE680),
    background = Color(0xFF0B0B0C),
    onBackground = Color(0xFFEDEAE0),
    surface = Color(0xFF161618),
    onSurface = Color(0xFFEDEAE0),
    surfaceVariant = Color(0xFF222225),
    onSurfaceVariant = Color(0xFFC9C6B8),
    outline = Color(0xFF48453A),
)

/** Tema Kitsunime: selalu gelap, hitam dengan aksen kuning. */
@Composable
fun KitsunimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
