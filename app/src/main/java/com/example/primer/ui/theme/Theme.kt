package com.example.primer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    onBackground = OnBackground,
    onSurface = OnBackground,
    primary = Accent,
    onPrimary = Background
)

@Composable
fun PrimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
