package org.example

import org.example.entities.User

/**
 * Singleton pour gérer l'utilisateur connecté à travers l'application
 */
object UserManager {
    // L'utilisateur actuellement connecté
    private var _currentUser: User? = null
    
    // Propriété pour obtenir l'utilisateur courant
    val currentUser: User?
        get() = _currentUser
    
    // Méthode pour définir l'utilisateur courant
    fun setCurrentUser(user: User?) {
        _currentUser = user
    }
    
    // Méthode pour vérifier si un utilisateur est connecté
    fun isUserLoggedIn(): Boolean {
        return _currentUser != null
    }
    
    // Méthode pour déconnecter l'utilisateur
    fun logout() {
        _currentUser = null
    }
}