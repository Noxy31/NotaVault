package org.example

import org.example.entities.Album
import org.example.entities.Albums
import org.example.entities.Colors
import org.example.entities.Images
import org.ktorm.dsl.*
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.entity.find

/**
 * Repository pour gérer les albums
 */
class AlbumRepository {
    // Utiliser la connexion existante
    private val database = org.example.Database.connect()
    
    /**
     * Récupère tous les albums utilisés par un utilisateur
     * Note: comme la table Albums n'a pas de référence directe à l'utilisateur,
     * nous cherchons les albums qui sont référencés par au moins une image de l'utilisateur
     */
    fun getUserAlbums(userId: Int): List<Album> {
        try {
            // Pour l'instant, retournons simplement tous les albums
            // car il n'y a pas de notion d'appartenance d'un album à un utilisateur
            return database.sequenceOf(Albums).toList()
        } catch (e: Exception) {
            println("Erreur lors de la récupération des albums: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Récupère un album par son ID
     */
    fun getAlbumById(albumId: Int): Album? {
        try {
            return database.sequenceOf(Albums).find { it.idAlbum eq albumId }
        } catch (e: Exception) {
            println("Erreur lors de la récupération de l'album: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Crée un nouvel album
     * Note: Le paramètre userId n'est pas stocké dans la table Albums, mais il est utilisé
     * pour vérifier que l'utilisateur existe
     */
    fun createAlbum(albumName: String, colorId: Int, userId: Int): Int? {
        try {
            // Insérer l'album
            val generatedId = database.insertAndGenerateKey(Albums) {
                set(Albums.albumName, albumName)
                set(Albums.idColor, colorId)
            } as Int?
            
            return generatedId
        } catch (e: Exception) {
            println("Erreur lors de la création de l'album: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Renomme un album
     */
    fun renameAlbum(albumId: Int, newName: String): Boolean {
        try {
            val affectedRows = database.update(Albums) {
                set(Albums.albumName, newName)
                where { Albums.idAlbum eq albumId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du renommage de l'album: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Change la couleur d'un album
     */
    fun changeAlbumColor(albumId: Int, colorId: Int): Boolean {
        try {
            val affectedRows = database.update(Albums) {
                set(Albums.idColor, colorId)
                where { Albums.idAlbum eq albumId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du changement de couleur de l'album: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Supprime un album
     * Note: Cela ne supprime pas les images associées à l'album,
     * mais les dissocie simplement de l'album.
     */
    fun deleteAlbum(albumId: Int): Boolean {
        try {
            // D'abord, mettre à jour toutes les images de cet album pour qu'elles n'aient pas d'album
            database.update(Images) {
                set(Images.idAlbum, null)
                where { Images.idAlbum eq albumId }
            }
            
            // Ensuite, supprimer l'album
            val affectedRows = database.delete(Albums) { it.idAlbum eq albumId }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors de la suppression de l'album: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Compte le nombre d'images dans un album
     */
    fun countImagesInAlbum(albumId: Int): Int {
        try {
            val result = database.from(Images)
                .select(count())
                .where { Images.idAlbum eq albumId }
                .map { row -> row.getInt(1) }
                .firstOrNull() ?: 0
            
            return result
        } catch (e: Exception) {
            println("Erreur lors du comptage des images dans l'album: ${e.message}")
            e.printStackTrace()
            return 0
        }
    }
}