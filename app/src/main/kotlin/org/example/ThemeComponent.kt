package org.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Thème sombre personnalisé pour l'application
 */
@Composable
fun ModernDarkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = darkColors(
        primary = Color(0xFF9D72FF),        // Violet clair
        primaryVariant = Color(0xFF7C4DFF),  // Violet plus foncé
        secondary = Color(0xFF03DAC5),      // Turquoise
        background = Color(0xFF1A1A2E),     // Bleu très foncé
        surface = Color(0xFF2A2A3A),        // Gris foncé avec teinte bleue
        error = Color(0xFFCF6679),          // Rouge clair
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onBackground = Color.White,
        onSurface = Color.White,
        onError = Color.Black
    )

    MaterialTheme(
        colors = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}