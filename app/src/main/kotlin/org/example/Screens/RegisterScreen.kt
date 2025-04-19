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
fun RegisterScreen(
    onRegisterSuccess: (secretSentence: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val userRepository = remember { UserRepository() }
    
    val passwordFocusRequester = remember { FocusRequester() }
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
                    .width(400.dp)
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
                                text = "📝",
                                fontSize = MaterialTheme.typography.h3.fontSize,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Titre
                    Text(
                        text = "Créer un compte",
                        style = MaterialTheme.typography.h4,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Remplissez les champs pour vous inscrire",
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
                            onNext = { passwordFocusRequester.requestFocus() }
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
                    
                    // Champ mot de passe
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            errorMessage = ""
                        },
                        label = { Text("Mot de passe") },
                        leadingIcon = {
                            Text(
                                text = "🔑",
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
                            .focusRequester(passwordFocusRequester),
                        enabled = !isLoading
                    )
                    
                    // Champ confirmation mot de passe
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it 
                            errorMessage = ""
                        },
                        label = { Text("Confirmer le mot de passe") },
                        leadingIcon = {
                            Text(
                                text = "🔑",
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
                                    attemptRegister(
                                        username = username,
                                        password = password,
                                        confirmPassword = confirmPassword,
                                        coroutineScope = coroutineScope,
                                        userRepository = userRepository,
                                        onLoading = { isLoading = it },
                                        onError = { errorMessage = it },
                                        onSuccess = onRegisterSuccess
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Bouton d'inscription
                    Button(
                        onClick = {
                            if (!isLoading) {
                                attemptRegister(
                                    username = username,
                                    password = password,
                                    confirmPassword = confirmPassword,
                                    coroutineScope = coroutineScope,
                                    userRepository = userRepository,
                                    onLoading = { isLoading = it },
                                    onError = { errorMessage = it },
                                    onSuccess = onRegisterSuccess
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
                                text = "S'INSCRIRE",
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
                        text = "Vous avez déjà un compte ?",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onBackToLogin,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Se connecter",
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
 * Fonction pour tenter l'inscription
 */
private fun attemptRegister(
    username: String,
    password: String,
    confirmPassword: String,
    coroutineScope: CoroutineScope,
    userRepository: UserRepository,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    // Validation des champs
    if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
        onError("Veuillez remplir tous les champs.")
        return
    }
    
    if (username.length < 3) {
        onError("Le nom d'utilisateur doit contenir au moins 3 caractères.")
        return
    }
    
    if (password.length < 6) {
        onError("Le mot de passe doit contenir au moins 6 caractères.")
        return
    }
    
    if (password != confirmPassword) {
        onError("Les mots de passe ne correspondent pas.")
        return
    }
    
    // Lancer l'inscription dans une coroutine
    coroutineScope.launch {
        onLoading(true)
        
        try {
            // Effectuer l'inscription dans un thread IO
            val (user, secretSentence) = withContext(Dispatchers.IO) {
                userRepository.createUser(username, password)
            }
            
            // Vérifier le résultat
            if (user != null && secretSentence != null) {
                // Inscription réussie
                onSuccess(secretSentence)
            } else {
                // Échec de l'inscription
                onError(secretSentence ?: "Erreur lors de l'inscription.")
            }
        } catch (e: Exception) {
            // Erreur lors de l'inscription
            onError("Erreur lors de l'inscription: ${e.message}")
        } finally {
            onLoading(false)
        }
    }
}

@Preview
@Composable
fun PreviewRegisterScreen() {
    RegisterScreen(
        onRegisterSuccess = {},
        onBackToLogin = {}
    )
}