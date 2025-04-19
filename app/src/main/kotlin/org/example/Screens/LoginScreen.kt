package org.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import org.example.UserRepository

// Définition du thème sombre personnalisé
private val DarkColorPalette = darkColors(
    primary = Color(0xFF9D72FF),        // Violet plus clair
    primaryVariant = Color(0xFFB995FF), // Violet encore plus clair
    secondary = Color(0xFF03DAC6),      // Turquoise inchangé
    background = Color(0xFF121212),     // Noir inchangé
    surface = Color(0xFF1E1E1E),        // Gris foncé inchangé
    onPrimary = Color.White,            // Blanc inchangé
    onSecondary = Color.Black,          // Noir inchangé
    onBackground = Color.White,         // Blanc inchangé
    onSurface = Color.White             // Blanc inchangé
)

@Composable
fun ModernDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        typography = Typography(
            h4 = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            button = MaterialTheme.typography.button.copy(
                fontWeight = FontWeight.Bold
            )
        ),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val userRepository = remember { UserRepository() }
    
    val passwordFocusRequester = remember { FocusRequester() }
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
            
            // Contenu principal avec scroll
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = maxWidth
                val height = maxHeight
                
                val cardWidth = (width * 0.9f).coerceAtMost(400.dp)
                
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo personnalisé
                            Box(
                            modifier = Modifier
                                .size(300.dp)
                                .padding(0.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {

                            Image(
                                painter = painterResource("NVWhite.png"),
                                contentDescription = "NoteVault Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            

                        }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = MaterialTheme.colors.surface,
                        elevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                        ) {
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Titre
                            Text(
                                text = "Bienvenue",
                                style = MaterialTheme.typography.h4,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "Connectez-vous pour continuer",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Champ utilisateur
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
                                    backgroundColor = Color(0xFF2A2A3A),
                                    focusedLabelColor = Color(0xFFB995FF)
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
                                            color = Color(0xFFB995FF),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { 
                                        focusManager.clearFocus()
                                        if (!isLoading) {
                                            attemptLogin(
                                                username = username,
                                                password = password,
                                                coroutineScope = coroutineScope,
                                                userRepository = userRepository,
                                                onLoading = { isLoading = it },
                                                onError = { errorMessage = it },
                                                onSuccess = onLoginSuccess
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
                                    backgroundColor = Color(0xFF2A2A3A),
                                    focusedLabelColor = Color(0xFFB995FF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(passwordFocusRequester),
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
                                        .padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Bouton de connexion
                            Button(
                                onClick = {
                                    if (!isLoading) {
                                        attemptLogin(
                                            username = username,
                                            password = password,
                                            coroutineScope = coroutineScope,
                                            userRepository = userRepository,
                                            onLoading = { isLoading = it },
                                            onError = { errorMessage = it },
                                            onSuccess = onLoginSuccess
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
                                        text = "SE CONNECTER",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Lien Mot de passe oublié
                            TextButton(
                                onClick = onForgotPasswordClick,
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = "Mot de passe oublié ?",
                                    color = Color(0xFFB995FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "Vous n'avez pas de compte ?",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        TextButton(
                            onClick = onRegisterClick,
                            enabled = !isLoading
                        ) {
                            Text(
                                text = "Créer un compte",
                                color = Color(0xFFB995FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fonction pour tenter la connexion à la base de données
 */
private fun attemptLogin(
    username: String,
    password: String,
    coroutineScope: CoroutineScope,
    userRepository: UserRepository,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    // Validation des champs
    if (username.isEmpty() || password.isEmpty()) {
        onError("Veuillez remplir tous les champs.")
        return
    }
    
    // Lancer l'authentification dans une coroutine
    coroutineScope.launch {
        onLoading(true)
        
        try {
            // Effectuer l'authentification dans un thread IO
            val user = withContext(Dispatchers.IO) {
                userRepository.authenticateUser(username, password)
            }
            
            // Vérifier le résultat
            if (user != null) {
                // Authentification réussie
                // Important: Stocker l'utilisateur dans le UserManager
                UserManager.setCurrentUser(user)
                onSuccess()
            } else {
                // Échec de l'authentification
                onError("Identifiants incorrects.")
            }
        } catch (e: Exception) {
            // Erreur lors de l'authentification
            onError("Erreur lors de la connexion: ${e.message}")
        } finally {
            onLoading(false)
        }
    }
}

@Preview
@Composable
fun PreviewLoginScreen() {
    LoginScreen(
        onLoginSuccess = {},
        onRegisterClick = {},
        onForgotPasswordClick = {}
    )
}