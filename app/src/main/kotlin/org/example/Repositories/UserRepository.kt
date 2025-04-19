package org.example

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import org.example.entities.User
import org.example.entities.Users
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

/**
 * Repository pour gérer les opérations liées aux utilisateurs dans la base de données
 */
class UserRepository {
    private val argon2: Argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id, // Type d'Argon2 (id est le plus recommandé)
        16,  // Salt length
        32   // Hash length
    )
    
    // Générateur de nombres aléatoires sécurisé pour la phrase secrète
    private val secureRandom = SecureRandom().asKotlinRandom()
    
    /**
     * Vérifie les identifiants d'un utilisateur pour la connexion
     * 
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @return User si connexion réussie, null sinon
     */
    fun authenticateUser(username: String, password: String): User? {
        val db = Database.connect()
        
        try {
            // Rechercher l'utilisateur par nom d'utilisateur
            val user = db.sequenceOf(Users)
                .firstOrNull { it.userLogin eq username }
            
            if (user != null) {
                // Vérifier le mot de passe avec Argon2
                val storedHash = user.userPassword
                val passwordMatches = argon2.verify(storedHash, password.toCharArray())
                
                // Si le mot de passe correspond
                if (passwordMatches) {
                    return user
                }
            }
            
            return null
        } catch (e: Exception) {
            println("Erreur lors de l'authentification: ${e.message}")
            return null
        }
    }
    
    /**
     * Vérifie si un nom d'utilisateur existe déjà
     */
    fun usernameExists(username: String): Boolean {
        val db = Database.connect()
        
        return db.sequenceOf(Users)
            .any { it.userLogin eq username }
    }
    
    /**
     * Génère une phrase secrète aléatoire
     */
    fun generateSecretSentence(): String {
        val adjectives = listOf("rouge", "bleu", "vert", "jaune", "grand", "petit", "lumineux", "doux", "rapide", "lent", "calme", "bruyant", "joyeux", "triste", "mystérieux")
        val nouns = listOf("chien", "chat", "oiseau", "arbre", "maison", "voiture", "livre", "montagne", "soleil", "rivière", "lune", "étoile", "fleur")
        val verbs = listOf("court", "saute", "vole", "nage", "brille", "dort", "chante", "danse", "écrit", "lit", "parle", "écoute", "regarde")
        
        val adjective = adjectives[secureRandom.nextInt(adjectives.size)]
        val noun = nouns[secureRandom.nextInt(nouns.size)]
        val verb = verbs[secureRandom.nextInt(verbs.size)]
        
        return "Le $adjective $noun $verb aujourd'hui."
    }
    
    /**
     * Crée un nouvel utilisateur
     * 
     * @param username Nom d'utilisateur
     * @param password Mot de passe
     * @return L'utilisateur créé si réussi, null sinon
     */
    fun createUser(username: String, password: String): Pair<User?, String?> {
        val db = Database.connect()
        
        try {
            if (usernameExists(username)) {
                return Pair(null, "Ce nom d'utilisateur existe déjà.")
            }
            
            val secretSentence = generateSecretSentence()
            
            val hashedPassword = hashWithArgon2(password)
            val hashedSecretSentence = hashWithArgon2(secretSentence)
            
            val insertedId = db.insert(Users) {
                set(it.userLogin, username)
                set(it.userPassword, hashedPassword)
                set(it.userSecretSentence, hashedSecretSentence)
            }
            
            if (insertedId > 0) {
                val newUser = db.sequenceOf(Users)
                    .firstOrNull { it.idUser eq insertedId.toInt() }
                
                return Pair(newUser, secretSentence)
            }
            
            return Pair(null, "Erreur lors de la création de l'utilisateur.")
        } catch (e: Exception) {
            println("Erreur lors de la création de l'utilisateur: ${e.message}")
            return Pair(null, "Erreur: ${e.message}")
        }
    }
    
    /**
     * Réinitialise le mot de passe d'un utilisateur
     * 
     * @param username Nom d'utilisateur
     * @param secretSentence Phrase secrète
     * @param newPassword Nouveau mot de passe
     * @return true si réussi, false sinon
     */
    fun resetPassword(username: String, secretSentence: String, newPassword: String): Boolean {
        val db = Database.connect()
        
        try {
            val user = db.sequenceOf(Users)
                .firstOrNull { it.userLogin eq username }
            
            if (user != null) {
                val secretMatches = argon2.verify(user.userSecretSentence, secretSentence.toCharArray())
                
                if (secretMatches) {
                    val hashedPassword = hashWithArgon2(newPassword)
                    
                    db.update(Users) {
                        set(it.userPassword, hashedPassword)
                        where { it.idUser eq user.idUser }
                    }
                    
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            println("Erreur lors de la réinitialisation du mot de passe: ${e.message}")
            return false
        }
    }
    
    /**
     * Fonction utilitaire pour hasher une chaîne avec Argon2
     */
    private fun hashWithArgon2(input: String): String {
    // En Java/Kotlin, la lambda pour Argon2 est différente
    return argon2.hash(
        2,      // Nombre d'itérations
        65536,  // Mémoire utilisée en KiB (64 MB)
        1,      // Parallélisme (nombre de threads)
        input.toCharArray()  // Entrée directe sans lambda
    )
}
}