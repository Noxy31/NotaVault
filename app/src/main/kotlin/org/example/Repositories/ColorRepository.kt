package org.example

import org.example.entities.Color
import org.example.entities.Colors
import org.ktorm.dsl.*
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.entity.find

/**
 * Repository pour gérer les couleurs
 */
class ColorRepository {
    // Utiliser la connexion existante
    private val database = org.example.Database.connect()
    
    /**
     * Récupère toutes les couleurs disponibles
     */
    fun getAllColors(): List<Color> {
        try {
            return database.sequenceOf(Colors).toList()
        } catch (e: Exception) {
            println("Erreur lors de la récupération des couleurs: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Récupère une couleur par son ID
     */
    fun getColorById(colorId: Int): Color? {
        try {
            return database.sequenceOf(Colors).find { it.idColor eq colorId }
        } catch (e: Exception) {
            println("Erreur lors de la récupération de la couleur: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Ajoute une couleur prédéfinie si elle n'existe pas déjà
     * Utile pour initialiser des couleurs par défaut
     */
    fun addPredefinedColorIfNotExists(colorName: String, colorHexa: String): Int? {
        try {
            // Vérifier si la couleur existe déjà
            val existingColor = database.sequenceOf(Colors)
                .find { (Colors.colorName eq colorName) or (Colors.colorHexa eq colorHexa) }
            
            if (existingColor != null) {
                return existingColor.idColor
            }
            
            // Sinon, l'ajouter
            return database.insertAndGenerateKey(Colors) {
                set(Colors.colorName, colorName)
                set(Colors.colorHexa, colorHexa)
            } as Int?
        } catch (e: Exception) {
            println("Erreur lors de l'ajout de la couleur prédéfinie: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Initialise les couleurs prédéfinies pour l'application
     */
    fun initPredefinedColors() {
        val predefinedColors = listOf(
            Pair("Rouge", "#e53935"),
            Pair("Rose", "#d81b60"),
            Pair("Violet", "#8e24aa"),
            Pair("Indigo", "#3949ab"),
            Pair("Bleu", "#1e88e5"),
            Pair("Cyan", "#00acc1"),
            Pair("Vert", "#43a047"),
            Pair("Lime", "#7cb342"),
            Pair("Jaune", "#fdd835"),
            Pair("Orange", "#fb8c00"),
            Pair("Marron", "#6d4c41"),
            Pair("Gris", "#757575")
        )
        
        predefinedColors.forEach { (name, hexa) ->
            addPredefinedColorIfNotExists(name, hexa)
        }
    }
}