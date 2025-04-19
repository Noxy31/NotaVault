package org.example

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import org.example.entities.Note
import org.example.entities.Notes
import org.example.entities.User
import org.example.entities.Colors
import org.ktorm.dsl.*
import org.ktorm.entity.*
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Repository pour gérer les opérations liées aux notes dans la base de données
 */
class NoteRepository {
    private val argon2: Argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id,
        16,  // Salt length
        32   // Hash length
    )
    
    // Clé utilisée pour le chiffrement (idéalement, cette clé devrait être unique par utilisateur)
    private var encryptionKey: String? = null
    
    // Cache pour les notes déchiffrées afin d'éviter des déchiffrements répétitifs
    private val decryptionCache = mutableMapOf<Int, Pair<String, String>>()
    
    /**
     * Définit la clé de chiffrement basée sur l'utilisateur connecté
     * @param user L'utilisateur connecté
     */
    fun setEncryptionKey(user: User) {
        encryptionKey = user.userLogin + "_" + user.idUser + "_secureKey"
        // Vider le cache quand on change d'utilisateur
        clearDecryptionCache()
    }
    
    /**
     * Vide le cache de déchiffrement
     */
    fun clearDecryptionCache() {
        decryptionCache.clear()
    }
    
    /**
     * Récupère toutes les notes d'un utilisateur, triées par date de création (plus récentes en premier)
     * @param userId ID de l'utilisateur
     * @return Liste des notes de l'utilisateur
     */
    fun getUserNotes(userId: Int): List<Note> {
        val db = Database.connect()
        
        return db.sequenceOf(Notes)
            .filter { (it.idUser eq userId) and (it.noteDeleteDate.isNull()) }
            .sortedByDescending { it.noteCreationDate }
            .toList()
    }
    
    /**
     * Recherche des notes en fonction de termes dans le titre ou le contenu
     * @param userId ID de l'utilisateur
     * @param searchTerm Terme de recherche
     * @return Liste des notes correspondant à la recherche
     */
    fun searchNotes(userId: Int, searchTerm: String): List<Note> {
        val db = Database.connect()
        val searchPattern = "%$searchTerm%"
        
        return db.sequenceOf(Notes)
            .filter { 
                (it.idUser eq userId) and 
                (it.noteDeleteDate.isNull()) and 
                ((it.noteTitle like searchPattern) or (it.noteContent like searchPattern))
            }
            .sortedByDescending { it.noteCreationDate }
            .toList()
    }
    
    /**
     * Crée une nouvelle note
     * @param userId ID de l'utilisateur
     * @param title Titre de la note
     * @param content Contenu de la note
     * @param colorId ID de la couleur
     * @return Note créée si réussie, null sinon
     */
    fun createNote(userId: Int, title: String, content: String, colorId: Int): Note? {
        if (encryptionKey == null) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        val db = Database.connect()
        
        try {
            println("Début de la création d'une note pour l'utilisateur $userId")
            println("Titre: $title, Contenu: $content, Couleur: $colorId")
            
            val encryptedTitle = encryptWithAES(title)
            val encryptedContent = encryptWithAES(content)
            val now = LocalDateTime.now(ZoneId.systemDefault())
            println(ZoneId.systemDefault())
            println("Date actuelle: $now")
            println("Titre chiffré: ${encryptedTitle.take(20)}...")
            
            val insertedId = db.insert(Notes) {
                set(Notes.noteTitle, encryptedTitle)
                set(Notes.noteContent, encryptedContent)
                set(Notes.noteCreationDate, now)
                set(Notes.idColor, colorId)
                set(Notes.idUser, userId)
            }
            
            println("Note insérée avec ID: $insertedId")
            
            if (insertedId > 0) {
                val newNote = db.sequenceOf(Notes)
                    .firstOrNull { it.idNote eq insertedId.toInt() }
                println("Note récupérée: $newNote")
                
                // Ajouter au cache de déchiffrement pour éviter un déchiffrement ultérieur
                if (newNote != null) {
                    decryptionCache[newNote.idNote] = Pair(title, content)
                }
                
                return newNote
            }
            
            println("Échec: aucune note n'a été créée")
            return null
        } catch (e: Exception) {
            println("Erreur lors de la création de la note: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Met à jour une note existante
     * @param noteId ID de la note
     * @param title Nouveau titre (ou null pour ne pas modifier)
     * @param content Nouveau contenu (ou null pour ne pas modifier)
     * @param colorId Nouvelle couleur (ou null pour ne pas modifier)
     * @return true si mise à jour réussie, false sinon
     */
    fun updateNote(noteId: Int, title: String? = null, content: String? = null, colorId: Int? = null): Boolean {
        if (encryptionKey == null && (title != null || content != null)) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        val db = Database.connect()
        
        try {
            db.update(Notes) {
                where { it.idNote eq noteId }
                
                title?.let { titleValue ->
                    set(Notes.noteTitle, encryptWithAES(titleValue))
                }
                
                content?.let { contentValue ->
                    set(Notes.noteContent, encryptWithAES(contentValue))
                }
                
                colorId?.let { colorIdValue ->
                    set(Notes.idColor, colorIdValue)
                }
                
                set(Notes.noteUpdateDate, LocalDateTime.now())
            }
            
            // Mettre à jour le cache si title ou content a été modifié
            if (title != null || content != null) {
                val currentCache = decryptionCache[noteId]
                if (currentCache != null) {
                    val newTitle = title ?: currentCache.first
                    val newContent = content ?: currentCache.second
                    decryptionCache[noteId] = Pair(newTitle, newContent)
                }
            }
            
            return true
        } catch (e: Exception) {
            println("Erreur lors de la mise à jour de la note: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
    * Supprime une note (hard delete au lieu de soft delete)
    * @param noteId ID de la note à supprimer
    * @return true si suppression réussie, false sinon
    */
    fun deleteNote(noteId: Int): Boolean {
        val db = Database.connect()
        
        try {
            // Exécuter la requête DELETE
            val deletedRows = db.delete(Notes) {
                it.idNote eq noteId
            }
            
            // Retirer du cache
            decryptionCache.remove(noteId)
            
            // Si au moins une ligne a été supprimée, la suppression est réussie
            return deletedRows > 0
        } catch (e: Exception) {
            println("Erreur lors de la suppression de la note: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun getNotesByColor(userId: Int, colorId: Int): List<Note> {
        val db = Database.connect()
        
        try {
            return db.sequenceOf(Notes)
                .filter { (it.idUser eq userId) and (it.idColor eq colorId) and it.noteDeleteDate.isNull() }
                .sortedByDescending { it.noteCreationDate }
                .toList()
        } catch (e: Exception) {
            println("Erreur lors de la récupération des notes par couleur: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Récupère une note par son ID
     * @param noteId ID de la note
     * @return La note si trouvée, null sinon
     */
    fun getNoteById(noteId: Int): Note? {
        val db = Database.connect()
        
        return db.sequenceOf(Notes)
            .firstOrNull { it.idNote eq noteId }
    }
    
    /**
     * Récupère toutes les couleurs disponibles
     * @return Liste des couleurs
     */
    fun getAvailableColors(): List<org.example.entities.Color> {
        val db = Database.connect()
        
        try {
            println("DEBUG: Tentative de récupération des couleurs")
            val colors = db.sequenceOf(Colors).toList()
            println("DEBUG: ${colors.size} couleurs récupérées")
            
            colors.forEach { color ->
                println("DEBUG: Couleur récupérée: ID=${color.idColor}, Nom=${color.colorName}, Hexa=${color.colorHexa}")
            }
            
            return colors
        } catch (e: Exception) {
            println("DEBUG: Erreur lors de la récupération des couleurs: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Déchiffre le titre et le contenu d'une note avec mise en cache
     * @param note Note à déchiffrer
     * @return Pair (titre déchiffré, contenu déchiffré) ou null en cas d'erreur
     */
    fun decryptNote(note: Note): Pair<String, String>? {
        if (encryptionKey == null) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        // Vérifier si la note est déjà dans le cache
        val cached = decryptionCache[note.idNote]
        if (cached != null) {
            return cached
        }
        
        try {
            val decryptedTitle = decryptWithAES(note.noteTitle)
            val decryptedContent = decryptWithAES(note.noteContent)
            
            val result = Pair(decryptedTitle, decryptedContent)
            // Stocker le résultat dans le cache
            decryptionCache[note.idNote] = result
            
            return result
        } catch (e: Exception) {
            println("Erreur lors du déchiffrement de la note: ${e.message}")
            e.printStackTrace()
            return Pair("", "")
        }
    }
    
    /**
     * Déchiffre un lot de notes en une seule opération
     * @param notes Liste des notes à déchiffrer
     * @return Map associant l'ID de la note à son contenu déchiffré
     */
    fun decryptNotesInBatch(notes: List<Note>): Map<Int, Pair<String, String>> {
        if (encryptionKey == null) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        val results = mutableMapOf<Int, Pair<String, String>>()
        
        notes.forEach { note ->
            // Vérifier d'abord si la note est dans le cache
            val cached = decryptionCache[note.idNote]
            if (cached != null) {
                results[note.idNote] = cached
            } else {
                try {
                    val decryptedTitle = decryptWithAES(note.noteTitle)
                    val decryptedContent = decryptWithAES(note.noteContent)
                    val pair = Pair(decryptedTitle, decryptedContent)
                    
                    // Ajouter au cache
                    decryptionCache[note.idNote] = pair
                    results[note.idNote] = pair
                } catch (e: Exception) {
                    println("Erreur lors du déchiffrement de la note ${note.idNote}: ${e.message}")
                    results[note.idNote] = Pair("Erreur de déchiffrement", "")
                }
            }
        }
        
        return results
    }
    
    /**
     * Chiffre une chaîne avec AES-GCM
     */
    private fun encryptWithAES(input: String): String {
        if (encryptionKey == null) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        // Utiliser CryptoUtils pour un chiffrement AES-GCM sécurisé
        return CryptoUtils.encrypt(input, encryptionKey!!)
    }
    
    /**
     * Déchiffre une chaîne avec AES-GCM
     */
    private fun decryptWithAES(input: String): String {
        if (encryptionKey == null) {
            throw IllegalStateException("La clé de chiffrement n'est pas définie")
        }
        
        // Utiliser CryptoUtils pour le déchiffrement
        return CryptoUtils.decrypt(input, encryptionKey!!)
    }
}