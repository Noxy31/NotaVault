package org.example
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.entities.User
import org.example.RegisterScreen
import org.example.LoginScreen
import org.example.SecretSentenceScreen
import org.example.ResetPasswordScreen
import org.example.MainScreen
import org.example.UserRepository
import org.example.UserManager

/**
 * Enumération des écrans disponibles dans l'application
 */
enum class Screen {
    LOGIN,
    REGISTER,
    SECRET_SENTENCE,
    RESET_PASSWORD,
    MAIN
}

/**
 * Composable qui gère la navigation entre les différents écrans de l'application
 */
@Composable
fun AppNavigation() {
    // État pour suivre l'écran actuel
    var currentScreen by remember { mutableStateOf(if (UserManager.isUserLoggedIn()) Screen.MAIN else Screen.LOGIN) }
    
    // État pour stocker la phrase secrète après l'inscription
    var secretSentence by remember { mutableStateOf("") }
    
    // Modifier cet état pour qu'il ne soit utilisé que pour forcer une récomposition
    var forceUpdate by remember { mutableStateOf(0) }
    
    // Afficher l'écran approprié en fonction de l'état actuel
    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    // L'utilisateur est déjà stocké dans UserManager à ce stade
                    // Forcer une mise à jour pour déclencher une recomposition
                    forceUpdate++
                    // Changer d'écran
                    currentScreen = Screen.MAIN
                },
                onRegisterClick = {
                    currentScreen = Screen.REGISTER
                },
                onForgotPasswordClick = {
                    currentScreen = Screen.RESET_PASSWORD
                }
            )
        }
        
        Screen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = { secretSentenceText ->
                    secretSentence = secretSentenceText
                    currentScreen = Screen.SECRET_SENTENCE
                },
                onBackToLogin = {
                    currentScreen = Screen.LOGIN
                }
            )
        }
        
        Screen.SECRET_SENTENCE -> {
            SecretSentenceScreen(
                secretSentence = secretSentence,
                onContinue = {
                    currentScreen = Screen.LOGIN
                }
            )
        }
        
        Screen.RESET_PASSWORD -> {
            ResetPasswordScreen(
                onResetSuccess = {
                    currentScreen = Screen.LOGIN
                },
                onBackToLogin = {
                    currentScreen = Screen.LOGIN
                }
            )
        }
        
        Screen.MAIN -> {
            // Utiliser l'utilisateur stocké dans UserManager
            val currentUser = UserManager.currentUser
            
            if (currentUser != null) {
                MainScreen(
                    currentUser = currentUser,
                    onLogout = {
                        // Déconnecter l'utilisateur
                        UserManager.logout()
                        // Retourner à l'écran de connexion
                        currentScreen = Screen.LOGIN
                    }
                )
            } else {
                // Si aucun utilisateur n'est connecté, retourner à l'écran de connexion
                currentScreen = Screen.LOGIN
            }
        }
    }
}