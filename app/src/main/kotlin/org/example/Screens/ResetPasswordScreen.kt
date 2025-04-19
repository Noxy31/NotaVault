package org.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.desktop.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.UserRepository

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ResetPasswordScreen(
    onResetSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var secretSentence by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    
    val secretSentenceFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    ModernDarkTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            Card(
                modifier = Modifier
                    .width(450.dp)
                    .padding(16.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo ou icône
                    Surface(
                        modifier = Modifier.size(70.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🔄",
                                fontSize = MaterialTheme.typography.h3.fontSize,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Titre
                    Text(
                        text = "Réinitialiser le mot de passe",
                        style = MaterialTheme.typography.h4,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Entrez votre nom d'utilisateur et votre phrase secrète",
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Champ nom d'utilisateur
                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it 
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Nom d'utilisateur") },
                        leadingIcon = {
                            Text(
                                text = "👤",
                                fontSize = MaterialTheme.typography.body1.fontSize,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { secretSentenceFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = MaterialTheme.colors.primary,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            backgroundColor = Color(0xFF2A2A3A)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    
                    // Champ phrase secrète
                    OutlinedTextField(
                        value = secretSentence,
                        onValueChange = { 
                            secretSentence = it 
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Phrase secrète") },
                        leadingIcon = {
                            Text(
                                text = "🔑",
                                fontSize = MaterialTheme.typography.body1.fontSize,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { newPasswordFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = MaterialTheme.colors.primary,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            backgroundColor = Color(0xFF2A2A3A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(secretSentenceFocusRequester),
                        enabled = !isLoading
                    )
                    
                    // Champ nouveau mot de passe
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { 
                            newPassword = it 
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Nouveau mot de passe") },
                        leadingIcon = {
                            Text(
                                text = "🔐",
                                fontSize = MaterialTheme.typography.body1.fontSize,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(
                                    text = if (passwordVisible) "Cacher" else "Voir",
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { confirmPasswordFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = MaterialTheme.colors.primary,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            backgroundColor = Color(0xFF2A2A3A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(newPasswordFocusRequester),
                        enabled = !isLoading
                    )
                    
                    // Champ confirmation nouveau mot de passe
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it 
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Confirmer le nouveau mot de passe") },
                        leadingIcon = {
                            Text(
                                text = "🔐",
                                fontSize = MaterialTheme.typography.body1.fontSize,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Text(
                                    text = if (confirmPasswordVisible) "Cacher" else "Voir",
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                focusManager.clearFocus()
                                if (!isLoading) {
                                    attemptResetPassword(
                                        username = username,
                                        secretSentence = secretSentence,
                                        newPassword = newPassword,
                                        confirmPassword = confirmPassword,
                                        coroutineScope = coroutineScope,
                                        userRepository = userRepository,
                                        onLoading = { isLoading = it },
                                        onError = { errorMessage = it; successMessage = "" },
                                        onSuccess = { 
                                            successMessage = "Mot de passe réinitialisé avec succès!"
                                            errorMessage = ""
                                            // Attendre 2 secondes avant de retourner à l'écran de connexion
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(2000)
                                                onResetSuccess()
                                            }
                                        }
                                    )
                                }
                            }
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = MaterialTheme.colors.primary,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            backgroundColor = Color(0xFF2A2A3A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(confirmPasswordFocusRequester),
                        enabled = !isLoading
                    )
                    
                    // Message d'erreur
                    AnimatedVisibility(
                        visible = errorMessage.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colors.error.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )
                    }
                    
                    // Message de succès
                    AnimatedVisibility(
                        visible = successMessage.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = successMessage,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bouton de réinitialisation
                    Button(
                        onClick = {
                            if (!isLoading) {
                                attemptResetPassword(
                                    username = username,
                                    secretSentence = secretSentence,
                                    newPassword = newPassword,
                                    confirmPassword = confirmPassword,
                                    coroutineScope = coroutineScope,
                                    userRepository = userRepository,
                                    onLoading = { isLoading = it },
                                    onError = { errorMessage = it; successMessage = "" },
                                    onSuccess = { 
                                        successMessage = "Mot de passe réinitialisé avec succès!"
                                        errorMessage = ""
                                        // Attendre 2 secondes avant de retourner à l'écran de connexion
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            onResetSuccess()
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colors.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "RÉINITIALISER",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Option pour retourner à la connexion (en bas)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Retourner à",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onBackToLogin,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "la page de connexion",
                            color = MaterialTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fonction pour tenter la réinitialisation du mot de passe
 */
private fun attemptResetPassword(
    username: String,
    secretSentence: String,
    newPassword: String,
    confirmPassword: String,
    coroutineScope: CoroutineScope,
    userRepository: UserRepository,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    // Validation des champs
    if (username.isEmpty() || secretSentence.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
        onError("Veuillez remplir tous les champs.")
        return
    }
    
    if (newPassword.length < 6) {
        onError("Le nouveau mot de passe doit contenir au moins 6 caractères.")
        return
    }
    
    if (newPassword != confirmPassword) {
        onError("Les mots de passe ne correspondent pas.")
        return
    }
    
    // Lancer la réinitialisation dans une coroutine
    coroutineScope.launch {
        onLoading(true)
        
        try {
            // Effectuer la réinitialisation dans un thread IO
            val success = withContext(Dispatchers.IO) {
                userRepository.resetPassword(username, secretSentence, newPassword)
            }
            
            // Vérifier le résultat
            if (success) {
                // Réinitialisation réussie
                onSuccess()
            } else {
                // Échec de la réinitialisation
                onError("Nom d'utilisateur ou phrase secrète incorrects.")
            }
        } catch (e: Exception) {
            // Erreur lors de la réinitialisation
            onError("Erreur lors de la réinitialisation: ${e.message}")
        } finally {
            onLoading(false)
        }
    }
}

@Preview
@Composable
fun PreviewResetPasswordScreen() {
    ResetPasswordScreen(
        onResetSuccess = {},
        onBackToLogin = {}
    )
}