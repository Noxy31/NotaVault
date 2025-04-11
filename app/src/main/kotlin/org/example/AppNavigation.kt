package org.example

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Ajouter ces imports explicites pour les composables
import org.example.LoginScreen
import org.example.ModernDarkTheme
// Si vous avez créé ces autres fichiers, ajoutez-les ici
// sinon, vous devrez créer ces fichiers
import org.example.RegisterScreen
import org.example.SecretSentenceScreen
import org.example.ResetPasswordScreen

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
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    
    // État pour stocker la phrase secrète après l'inscription
    var secretSentence by remember { mutableStateOf("") }
    
    // Afficher l'écran approprié en fonction de l'état actuel
    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = { 
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
                onRegisterSuccess = { generatedSecretSentence ->
                    secretSentence = generatedSecretSentence
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
            // Ici, vous afficheriez votre écran principal de l'application
            // Pour l'instant, nous allons simplement afficher un message
            MainPlaceholder(
                onLogout = { 
                    currentScreen = Screen.LOGIN
                }
            )
        }
    }
}

/**
 * Écran principal temporaire
 */
@Composable
fun MainPlaceholder(onLogout: () -> Unit) {
    ModernDarkTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenue dans NoteVault",
                style = MaterialTheme.typography.h4,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Connexion réussie! Ici sera votre écran principal.",
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary
                )
            ) {
                Text("Se déconnecter")
            }
        }
    }
}