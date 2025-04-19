package org.example

import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel

/**
 * Gestionnaire de connexion à la base de données avec Ktorm
 */
object Database {
    private var database: Database? = null
    
    // Configuration de la base de données
    private const val DB_URL = "jdbc:mysql://localhost:3306/notevault?useSSL=false&serverTimezone=UTC"
    private const val DB_USER = "root"
    private const val DB_PASSWORD = "" 
    
    /**
     * Obtenir une connexion à la base de données Ktorm
     */
    fun connect(): Database {
        if (database == null) {
            try {
                // Charger le driver MySQL
                Class.forName("com.mysql.cj.jdbc.Driver")
                
                // Créer la connexion Ktorm
                database = Database.connect(
                    url = DB_URL,
                    user = DB_USER,
                    password = DB_PASSWORD,
                    logger = ConsoleLogger(LogLevel.DEBUG),
        
                )
                
                println("Connexion à la base de données réussie!")
            } catch (e: Exception) {
                println("Erreur lors de la connexion à la base de données: ${e.message}")
                throw e
            }
        }
        
        return database!!
    }
}