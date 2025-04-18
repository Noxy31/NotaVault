package org.example

import org.example.entities.Image
import org.example.entities.Images
import org.example.entities.User
import org.example.entities.Users
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
    private val database = Database.connect()
    
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
        userId: Int
    ): Int? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            // Créer une miniature
            // val thumbnail = createThumbnail(imageData, mimeType)
            
            // Chiffrer l'image
            val encryptedImage = CryptoUtils.encryptBinary(imageData, encryptionKey!!)
            
            // Insérer dans la base de données
            val generatedId = database.insertAndGenerateKey(Images) {
                set(it.imageName, imageName)
                set(it.imageData, encryptedImage.encryptedData)
                set(it.imageSalt, encryptedImage.saltHex)
                set(it.imageIv, encryptedImage.ivHex)
                set(it.imageMimeType, mimeType)
                set(it.imageCreationDate, LocalDateTime.now())
                set(it.idUser, userId)
            } as Int?
            
            return generatedId
        } catch (e: Exception) {
            println("Erreur lors de la sauvegarde de l'image: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Récupérer la liste des images du coffre pour un utilisateur
     */
      fun getUserImages(userId: Int): List<Image> {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            return database.sequenceOf(Images)
                .filter { it.idUser eq userId }
                .sortedByDescending { it.imageCreationDate }
                .toList()
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
                set(it.imageName, newName)
                where { it.idImage eq imageId }
            }
            return affectedRows > 0
        } catch (e: Exception) {
            println("Erreur lors du renommage de l'image: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}