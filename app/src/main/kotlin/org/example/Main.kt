package org.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun main() = application {
    // Vérifier la connexion à la base de données au démarrage
    testDatabaseConnection()
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "NotaVault",
        state = rememberWindowState()
    ) {
        // Utiliser notre système de navigation pour gérer les écrans
        AppNavigation()
    }
}

/**
 * Fonction pour tester la connexion à la base de données au démarrage
 */
private fun testDatabaseConnection() {
    runBlocking {
        withContext(Dispatchers.IO) {
            try {
                // Tenter d'établir une connexion à la base de données
                Database.connect()
                println("Connexion à la base de données réussie!")
            } catch (e: Exception) {
                println("Échec de la connexion à la base de données: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}