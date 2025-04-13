package org.example

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.util.Base64

/**
 * Utilitaire pour le chiffrement et déchiffrement AES-GCM
 */
object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16
    
    /**
     * Dérive une clé de chiffrement à partir d'un mot de passe et d'un sel
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val iterations = 65536 // Nombre élevé d'itérations pour ralentir les attaques par force brute
        val keyLength = 256
        
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }
    
    /**
     * Chiffre une chaîne avec AES-GCM
     * @param plaintext Texte à chiffrer
     * @param password Mot de passe ou clé de chiffrement
     * @return Chaîne chiffrée encodée en Base64
     */
    fun encrypt(plaintext: String, password: String): String {
        try {
            // Génère un sel aléatoire
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH)
            random.nextBytes(salt)
            
            // Génère un IV (vecteur d'initialisation) aléatoire
            val iv = ByteArray(IV_LENGTH)
            random.nextBytes(iv)
            
            // Dérive une clé à partir du mot de passe et du sel
            val key = deriveKey(password, salt)
            
            // Initialise le chiffrement
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
            
            // Chiffre les données
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine sel + IV + texte chiffré
            val result = ByteArray(salt.size + iv.size + ciphertext.size)
            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(iv, 0, result, salt.size, iv.size)
            System.arraycopy(ciphertext, 0, result, salt.size + iv.size, ciphertext.size)
            
            // Encode en Base64 et retourne
            return Base64.getEncoder().encodeToString(result)
        } catch (e: Exception) {
            println("Erreur lors du chiffrement: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Déchiffre une chaîne chiffrée avec AES-GCM
     * @param encryptedData Données chiffrées encodées en Base64
     * @param password Mot de passe ou clé de chiffrement
     * @return Texte déchiffré
     */
    fun decrypt(encryptedData: String, password: String): String {
        try {
            // Décode le Base64
            val decodedData = Base64.getDecoder().decode(encryptedData)
            
            // Vérifie que les données sont valides
            if (decodedData.size < SALT_LENGTH + IV_LENGTH) {
                throw IllegalArgumentException("Données chiffrées invalides ou corrompues")
            }
            
            // Extrait le sel, l'IV et le texte chiffré
            val salt = ByteArray(SALT_LENGTH)
            val iv = ByteArray(IV_LENGTH)
            val ciphertext = ByteArray(decodedData.size - SALT_LENGTH - IV_LENGTH)
            
            System.arraycopy(decodedData, 0, salt, 0, salt.size)
            System.arraycopy(decodedData, salt.size, iv, 0, iv.size)
            System.arraycopy(decodedData, salt.size + iv.size, ciphertext, 0, ciphertext.size)
            
            // Dérive la clé à partir du mot de passe et du sel
            val key = deriveKey(password, salt)
            
            // Initialise le déchiffrement
            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
            
            // Déchiffre et retourne
            val plaintext = cipher.doFinal(ciphertext)
            return String(plaintext, Charsets.UTF_8)
        } catch (e: Exception) {
            println("Erreur lors du déchiffrement: ${e.message}")
            e.printStackTrace()
            // Retourner une chaîne vide en cas d'erreur pour éviter les plantages
            return ""
        }
    }
}