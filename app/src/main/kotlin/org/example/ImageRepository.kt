package org.example

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.example.entities.User
import org.jetbrains.skia.Image as SkiaImage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import kotlin.concurrent.thread

/**
 * Structure pour représenter une image dans le coffre
 */
data class VaultImage(
    val idImage: Int,
    val imageName: String,
    val thumbnailData: ByteArray? = null,  // Miniature déchiffrée pour l'affichage rapide
    val mimeType: String,
    val creationDate: LocalDateTime,
    // Ne stocke pas l'image complète déchiffrée ici pour économiser la mémoire
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VaultImage

        if (idImage != other.idImage) return false
        if (imageName != other.imageName) return false
        if (thumbnailData != null) {
            if (other.thumbnailData == null) return false
            if (!thumbnailData.contentEquals(other.thumbnailData)) return false
        } else if (other.thumbnailData != null) return false
        if (mimeType != other.mimeType) return false
        if (creationDate != other.creationDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idImage
        result = 31 * result + imageName.hashCode()
        result = 31 * result + (thumbnailData?.contentHashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + creationDate.hashCode()
        return result
    }
}

/**
 * Repository pour gérer les images du coffre-fort
 */
class ImageRepository {
    private var encryptionKey: String? = null
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
            val thumbnail = createThumbnail(imageData, mimeType)
            
            // Chiffrer l'image et la miniature
            val encryptedImage = CryptoUtils.encryptBinary(imageData, encryptionKey!!)
            val encryptedThumbnail = thumbnail?.let { CryptoUtils.encryptBinary(it, encryptionKey!!) }
            
            // Insertion en base de données
            return database.useConnection { conn ->
                val sql = """
                    INSERT INTO images (
                        id_user, image_name, image_data, image_salt, image_iv, 
                        thumbnail_data, thumbnail_salt, thumbnail_iv, image_mime_type
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
                
                val stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
                
                // Paramètres
                var index = 1
                stmt.setInt(index++, userId)
                stmt.setString(index++, imageName)
                stmt.setBytes(index++, encryptedImage.encryptedData)
                stmt.setString(index++, encryptedImage.saltHex)
                stmt.setString(index++, encryptedImage.ivHex)
                
                if (encryptedThumbnail != null) {
                    stmt.setBytes(index++, encryptedThumbnail.encryptedData)
                    stmt.setString(index++, encryptedThumbnail.saltHex)
                    stmt.setString(index++, encryptedThumbnail.ivHex)
                } else {
                    stmt.setNull(index++, java.sql.Types.BLOB)
                    stmt.setNull(index++, java.sql.Types.VARCHAR)
                    stmt.setNull(index++, java.sql.Types.VARCHAR)
                }
                
                stmt.setString(index, mimeType)
                
                // Exécuter l'insertion
                val rowsAffected = stmt.executeUpdate()
                if (rowsAffected > 0) {
                    // Récupérer l'ID généré
                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        return@useConnection rs.getInt(1)
                    }
                }
                
                return@useConnection null
            }
        } catch (e: Exception) {
            println("Erreur lors de la sauvegarde de l'image: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Récupérer la liste des images du coffre pour un utilisateur (avec miniatures uniquement)
     */
    fun getUserImages(userId: Int): List<VaultImage> {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        val images = mutableListOf<VaultImage>()
        
        try {
            database.useConnection { conn ->
                val sql = """
                    SELECT id_image, image_name, thumbnail_data, thumbnail_salt, thumbnail_iv,
                           image_mime_type, image_creation_date
                    FROM images
                    WHERE id_user = ?
                    ORDER BY image_creation_date DESC
                """
                
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, userId)
                
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val idImage = rs.getInt("id_image")
                    val imageName = rs.getString("image_name")
                    val mimeType = rs.getString("image_mime_type")
                    val creationDate = rs.getTimestamp("image_creation_date").toLocalDateTime()
                    
                    // Déchiffrer la miniature si disponible
                    var thumbnailData: ByteArray? = null
                    val encryptedThumbnail = rs.getBytes("thumbnail_data")
                    
                    if (encryptedThumbnail != null && !rs.wasNull()) {
                        val thumbnailSalt = rs.getString("thumbnail_salt")
                        val thumbnailIv = rs.getString("thumbnail_iv")
                        
                        try {
                            thumbnailData = CryptoUtils.decryptBinary(
                                encryptedThumbnail, thumbnailSalt, thumbnailIv, encryptionKey!!
                            )
                        } catch (e: Exception) {
                            println("Erreur lors du déchiffrement de la miniature: ${e.message}")
                            // Continuer sans miniature en cas d'erreur
                        }
                    }
                    
                    images.add(
                        VaultImage(
                            idImage = idImage,
                            imageName = imageName,
                            thumbnailData = thumbnailData,
                            mimeType = mimeType,
                            creationDate = creationDate
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la récupération des images: ${e.message}")
            e.printStackTrace()
        }
        
        return images
    }
    
    /**
     * Récupérer une image complète
     */
    fun getImageData(imageId: Int): ByteArray? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            database.useConnection { conn ->
                val sql = """
                    SELECT image_data, image_salt, image_iv
                    FROM images
                    WHERE id_image = ?
                """
                
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, imageId)
                
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val encryptedData = rs.getBytes("image_data")
                    val salt = rs.getString("image_salt")
                    val iv = rs.getString("image_iv")
                    
                    return@useConnection CryptoUtils.decryptBinary(
                        encryptedData, salt, iv, encryptionKey!!
                    )
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la récupération de l'image: ${e.message}")
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * Récupérer la miniature d'une image
     */
    fun getImageThumbnail(imageId: Int): ByteArray? {
        if (encryptionKey == null) {
            throw IllegalStateException("Clé de chiffrement non définie")
        }
        
        try {
            database.useConnection { conn ->
                val sql = """
                    SELECT thumbnail_data, thumbnail_salt, thumbnail_iv
                    FROM images
                    WHERE id_image = ?
                """
                
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, imageId)
                
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val encryptedThumbnail = rs.getBytes("thumbnail_data")
                    if (encryptedThumbnail != null && !rs.wasNull()) {
                        val thumbnailSalt = rs.getString("thumbnail_salt")
                        val thumbnailIv = rs.getString("thumbnail_iv")
                        
                        return@useConnection CryptoUtils.decryptBinary(
                            encryptedThumbnail, thumbnailSalt, thumbnailIv, encryptionKey!!
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Erreur lors de la récupération de la miniature: ${e.message}")
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * Supprime une image
     */
    fun deleteImage(imageId: Int): Boolean {
        try {
            database.useConnection { conn ->
                val sql = "DELETE FROM images WHERE id_image = ?"
                
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, imageId)
                
                return@useConnection stmt.executeUpdate() > 0
            }
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
            return skiaImage.asImageBitmap()
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
            database.useConnection { conn ->
                val sql = "UPDATE images SET image_name = ? WHERE id_image = ?"
                
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, newName)
                stmt.setInt(2, imageId)
                
                return@useConnection stmt.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("Erreur lors du renommage de l'image: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}