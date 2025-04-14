package org.example

import androidx.compose.ui.graphics.Color

/**
 * Fonction utilitaire pour obtenir la couleur à partir du nom et/ou de la valeur hexadécimale
 */
fun getColorFromName(colorName: String, colorHexa: String? = null): Color {
    // Si on a fourni une valeur hexadécimale, l'utiliser directement
    if (colorHexa != null && colorHexa.isNotEmpty()) {
        try {
            // Pour être compatible avec la fonction de parsing de couleur d'Android et de Compose
            val hexCode = colorHexa.replace("#", "").trim()
            
            // Vérifier que c'est un code hexa valide
            if (hexCode.matches(Regex("[0-9A-Fa-f]{6}"))) {
                val r = hexCode.substring(0, 2).toInt(16)
                val g = hexCode.substring(2, 4).toInt(16)
                val b = hexCode.substring(4, 6).toInt(16)
                return Color(r, g, b, 255) // Alpha à 255 (opaque)
            }
        } catch (e: Exception) {
            println("DEBUG: Erreur lors du parsing de la couleur hexa: $colorHexa - ${e.message}")
        }
    }
    
    // Fallback sur les valeurs codées en dur
    return when (colorName.lowercase()) {
        "rouge" -> Color(0xFFFF5252)
        "bleu" -> Color(0xFF536DFE)
        "vert" -> Color(0xFF4CAF50)
        "jaune" -> Color(0xFFFFEB3B)
        "orange" -> Color(0xFFFF9800)
        "violet" -> Color(0xFF9C27B0)
        "rose" -> Color(0xFFE91E63)
        "turquoise" -> Color(0xFF00BCD4)
        "gris" -> Color(0xFF9E9E9E)
        else -> Color(0xFF9D72FF) // Couleur primaire par défaut
    }
}

/**
 * Fonction utilitaire pour obtenir la couleur hexadécimale à partir du nom de couleur
 */
fun getColorHexFromName(colorName: String): String {
    return when (colorName.lowercase()) {
        "rouge" -> "#FF5252"
        "bleu" -> "#536DFE"
        "vert" -> "#4CAF50"
        "jaune" -> "#FFEB3B"
        "orange" -> "#FF9800"
        "violet" -> "#9C27B0"
        "rose" -> "#E91E63"
        "turquoise" -> "#00BCD4"
        "gris" -> "#9E9E9E"
        else -> "#9D72FF" // Couleur primaire par défaut
    }
}