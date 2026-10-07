package com.aistudio.cozytown.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CozyColorScheme = lightColorScheme(
    primary = ColorTerra,
    onPrimary = Color.White,
    primaryContainer = ColorSand,
    onPrimaryContainer = ColorInk,
    secondary = ColorMint,
    onSecondary = Color.White,
    secondaryContainer = ColorPaperCard,
    onSecondaryContainer = ColorInk,
    background = ColorPaper,
    onBackground = ColorInk,
    surface = ColorPaperCard,
    onSurface = ColorInk,
    surfaceVariant = ColorPaper,
    onSurfaceVariant = ColorInkLight,
    outline = ColorBorder
)

@Composable
fun CozyTownTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyColorScheme,
        content = content
    )
}
