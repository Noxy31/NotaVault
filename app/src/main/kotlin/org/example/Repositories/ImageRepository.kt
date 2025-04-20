package org.example

import org.example.entities.Image
import org.example.entities.Images
import org.example.entities.User
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.entity.filter
import org.ktorm.entity.sortedByDescending
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkiaImage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import org.ktorm.dsl.*

/**
 * Repository pour gérer les images du coffre-fort
 */
class ImageRepository {
    private var encryptionKey: String? = null
    
    // Au lieu de créer une nouvelle connexion, utilisez celle déjà configurée
    private val database = org.example.Database.connect()
    
    // Configuration de la taille des miniatures
    private val THUMBNAIL_WIDTH = 200
    private val THUMBNAIL_HEIGHT = 200
    
    /**
     * Définir la clé de chiffrement basée sur l'utilisateur
     */
    fun setEncryptionKey(user: User) {
        // Utilise le même mécanisme que NoteRepository
        encryptionKey = "${user.userLogin}:${user.userPassword}"
    }
    
    /**
     * Créer une miniature à partir des données d'image originales
     */
    private fun createThumbnail(imageData: ByteArray, mimeType: String): ByteArray? {
        try {
            // Lire l'image
            val inputStream = ByteArrayInputStream(imageData)
            val originalImage = ImageIO.read(inputStream)
            
            if (originalImage == null) {
                println("Impossible de lire l'image pour créer une miniature")
                return null
            }
            
            // Calculer les dimensions pour préserver le ratio
            val originalWidth = originalImage.width
            val originalHeight = originalImage.height
            
            val ratio = Math.min(
                THUMBNAIL_WIDTH.toDouble() / originalWidth,
                THUMBNAIL_HEIGHT.toDouble() / originalHeight
            )
            
            val width = (originalWidth * ratio).toInt()
            val height = (originalHeight * ratio).toInt()
            
            // Créer la miniature
            val thumbImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val graphics = thumbImage.createGraphics()
            graphics.drawImage(originalImage, 0, 0, width, height, null)
            graphics.dispose()
            
            // Convertir en bytes
            val outputStream = ByteArrayOutputStream()
            val imageFormat = when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpeg"
                mimeType.contains("png") -> "png"
                else -> "jpeg"  // Format par défaut
            }
            
            ImageIO.write(thumbImage, imageFormat, outputStream)
            return outputStream.toByteArray()
        } catch (e: Exception) {
            println("Erreur lors de la création de la miniature: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Sauvegarder une image dans le coffre
     */
    fun saveImage(
        imageData: ByteArray,
        imageName: String,
        mimeType: String,
        userId: Int,
        albumId: Int? = null
    ): Int? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            // Chiffrer l'image
            val encryptedImage = CryptoUtils.encryptBinary(imageData, encryptionKey!!)
            
            // Insérer dans la base de données
            val generatedId = database.insertAndGenerateKey(Images) {
                set(Images.imageName, imageName)
                set(Images.imageData, encryptedImage.encryptedData)
                set(Images.imageSalt, encryptedImage.saltHex)
                set(Images.imageIv, encryptedImage.ivHex)
                set(Images.imageMimeType, mimeType)
                set(Images.imageCreationDate, LocalDateTime.now())
                set(Images.idUser, userId)
                if (albumId != null) {
                    set(Images.idAlbum, albumId)
                }
            } as Int?
            
            return generatedId
        } catch (e: Exception) {
            println("Erreur lors de la sauvegarde de l'image: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Récupérer la liste des images du coffre pour un utilisateur,
     * avec filtrage optionnel par album
     */
    fun getUserImages(userId: Int, albumId: Int? = null): List<Image> {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            var query = database.sequenceOf(Images)
                .filter { it.idUser eq userId }
            
            // Si un albumId est spécifié, filtrer par cet album
            if (albumId != null) {
                query = query.filter { it.idAlbum eq albumId }
            }
            
            return query.sortedByDescending { it.imageCreationDate }.toList()
        } catch (e: Exception) {
            println("Erreur lors de la récupération des images: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Récupérer une image complète (déchiffrée)
     */
    fun getImageData(imageId: Int): ByteArray? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            val result = database.from(Images)
                .select(Images.imageData, Images.imageSalt, Images.imageIv)
                .where { Images.idImage eq imageId }
                .map { row ->
                    val encryptedData = row[Images.imageData]
                    val salt = row[Images.imageSalt]
                    val iv = row[Images.imageIv]
                    
                    if (encryptedData != null && salt != null && iv != null) {
                        CryptoUtils.decryptBinary(encryptedData, salt, iv, encryptionKey!!)
                    } else {
                        null
                    }
                }
                .firstOrNull()
            
            return result
        } catch (e: Exception) {
            println("Erreur lors de la récupération de l'image: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Récupérer la miniature d'une image (déchiffrée à la volée)
     */
    fun getImageThumbnail(imageId: Int): ByteArray? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            val result = database.from(Images)
                .select(Images.imageData, Images.imageSalt, Images.imageIv, Images.imageMimeType)
                .where { Images.idImage eq imageId }
                .map { row ->
                    val encryptedData = row[Images.imageData]
                    val salt = row[Images.imageSalt]
                    val iv = row[Images.imageIv]
                    val mimeType = row[Images.imageMimeType]
                    
                    if (encryptedData != null && salt != null && iv != null && mimeType != null) {
                        val imageData = CryptoUtils.decryptBinary(encryptedData, salt, iv, encryptionKey!!)
                        createThumbnail(imageData, mimeType)
                    } else {
                        null
                    }
                }
                .firstOrNull()
            
            return result
        } catch (e: Exception) {
            println("Erreur lors de la récupération de la miniature: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Supprime une image
     */
    fun deleteImage(imageId: Int): Boolean {
        try {
            val affectedRows = database.delete(Images) { it.idImage eq imageId }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors de la suppression de l'image: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Convertit les données d'une miniature en ImageBitmap
     */
    fun thumbnailToImageBitmap(thumbnailData: ByteArray?): ImageBitmap? {
        if (thumbnailData == null) return null
        
        try {
            val skiaImage = SkiaImage.makeFromEncoded(thumbnailData)
            return skiaImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("Erreur lors de la conversion de la miniature en ImageBitmap: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Renomme une image
     */
    fun renameImage(imageId: Int, newName: String): Boolean {
        try {
            val affectedRows = database.update(Images) {
                set(Images.imageName, newName)
                where { Images.idImage eq imageId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du renommage de l'image: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Assigne une image à un album
     */
    fun assignImageToAlbum(imageId: Int, albumId: Int): Boolean {
        try {
            val affectedRows = database.update(Images) {
                set(Images.idAlbum, albumId)
                where { Images.idImage eq imageId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors de l'assignation de l'image à l'album: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Retire une image d'un album (sans la supprimer)
     */
    fun removeImageFromAlbum(imageId: Int): Boolean {
        try {
            val affectedRows = database.update(Images) {
                set(Images.idAlbum, null)
                where { Images.idImage eq imageId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du retrait de l'image de l'album: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Récupérer la liste des images d'un utilisateur qui n'appartiennent à aucun album
     */
    fun getUserImagesWithoutAlbum(userId: Int): List<Image> {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            return database.sequenceOf(Images)
                .filter { (it.idUser eq userId) and (it.idAlbum.isNull()) }
                .sortedByDescending { it.imageCreationDate }
                .toList()
        } catch (e: Exception) {
            println("Erreur lors de la récupération des images sans album: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Déplace toutes les images d'un album vers un autre
     */
    fun moveImagesFromAlbumToAlbum(sourceAlbumId: Int, destinationAlbumId: Int?): Boolean {
        try {
            val affectedRows = database.update(Images) {
                set(Images.idAlbum, destinationAlbumId)
                where { Images.idAlbum eq sourceAlbumId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du déplacement des images entre albums: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}