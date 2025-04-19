package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState  // Ajout de cet import
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay  // Ajout de cet import
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.entities.Color as NoteColor
import org.example.entities.Note
import org.example.entities.User
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun SidebarComponent(
    currentUser: User,
    notes: List<Note>,
    selectedNoteId: Int?,
    onNoteSelected: (Int) -> Unit,
    onAddNote: () -> Unit,
    onGoToVault: () -> Unit,
    onLogout: () -> Unit,
    noteRepository: NoteRepository
) {
    // Utiliser des états mémorisés pour éviter des recréations inutiles
    var activeTab by remember { mutableStateOf("notes") }
    
    // Nouvel état pour gérer l'affichage de la boîte de dialogue de mot de passe
    var showPasswordDialog by remember { mutableStateOf(false) }
    
    // État pour la recherche
    var searchQuery by remember { mutableStateOf("") }
    
    // État pour le défilement de la liste
    val listState = rememberLazyListState()
    
    // État pour déclencher le défilement vers le haut
    var shouldScrollToTop by remember { mutableStateOf(false) }
    
    // Le scope pour les coroutines
    val coroutineScope = rememberCoroutineScope()
    
    // Trier les notes par date de mise à jour ou date de création (les plus récentes en premier)
    val sortedNotes = remember(notes) {
        notes.sortedByDescending { note ->
            note.noteUpdateDate ?: note.noteCreationDate
        }
    }
    
    // Filtrer les notes en fonction de la recherche
    val filteredNotes = remember(sortedNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedNotes
        } else {
            val lowerQuery = searchQuery.lowercase()
            sortedNotes.filter { note ->
                try {
                    val decrypted = noteRepository.decryptNote(note)
                    if (decrypted != null) {
                        decrypted.first.lowercase().contains(lowerQuery) || 
                        decrypted.second.lowercase().contains(lowerQuery)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
    
    // Effet pour déclencher le défilement lorsque shouldScrollToTop change
    LaunchedEffect(shouldScrollToTop) {
        if (shouldScrollToTop && filteredNotes.isNotEmpty()) {
            listState.animateScrollToItem(0)
            shouldScrollToTop = false
        }
    }
    
    // Effet pour surveiller les modifications de notes, notamment lors des ajouts
    LaunchedEffect(notes.size) {
        if (notes.isNotEmpty()) {
            // Si c'est la première charge ou un ajout, on va au début de la liste
            shouldScrollToTop = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(Color(0xFF1A1A2E))
            .padding(vertical = 16.dp)
    ) {
        // Profile section
        key(currentUser.idUser) {
            ProfileSection(currentUser, onLogout)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tabs pour naviguer entre Notes et Coffre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF232338))
        ) {
            // Onglet Notes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (activeTab == "notes") MaterialTheme.colors.primary
                        else Color(0xFF232338)
                    )
                    .clickable { activeTab = "notes" }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Notes",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Notes",
                        color = Color.White
                    )
                }
            }
            
            // Onglet Coffre - Modifié pour afficher la boîte de dialogue de mot de passe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (activeTab == "vault") MaterialTheme.colors.primary
                        else Color(0xFF232338)
                    )
                    .clickable { 
                        // Afficher la boîte de dialogue de mot de passe au lieu d'aller directement au coffre
                        showPasswordDialog = true
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Coffre",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Coffre",
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contenu de l'onglet actif
        if (activeTab == "notes") {
            // Contenu de l'onglet Notes
            
            // Add new note button
            Button(
                onClick = {
                    onAddNote()
                    // Déclencher le défilement vers le haut après l'ajout d'une note
                    coroutineScope.launch {
                        // Petit délai pour permettre la mise à jour de la liste
                        delay(300)
                        shouldScrollToTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nouvelle note",
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Champ de recherche
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher dans les notes") },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    placeholderColor = Color.White.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    backgroundColor = Color(0xFF232338)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes list avec résultats de recherche
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mes notes",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                    
                    // Afficher le nombre de résultats si recherche active
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "${filteredNotes.size} résultat(s)",
                            style = MaterialTheme.typography.caption,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (filteredNotes.isEmpty() && searchQuery.isNotEmpty()) {
                    // Message quand aucun résultat n'est trouvé
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucune note ne correspond à votre recherche",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                } else {
                    // Utiliser LazyColumn avec items keyed pour optimiser le rendu
                    LazyColumn(
                        state = listState // Utiliser l'état pour contrôler le défilement
                    ) {
                        items(
                            items = filteredNotes,
                            key = { it.idNote }  // Utiliser l'ID comme clé pour le recyclage
                        ) { note ->
                            val isSelected = note.idNote == selectedNoteId
                            
                            // Mémoriser le résultat du déchiffrement pour éviter de le refaire
                            // Utiliser la note.idNote comme clé pour invalider si la note change
                            val decryptedPair = remember(note.idNote, note.noteUpdateDate) {
                                try {
                                    noteRepository.decryptNote(note) ?: Pair("", "")
                                } catch (e: Exception) {
                                    Pair("Erreur de déchiffrement", "")
                                }
                            }
                            
                            OptimizedNoteCard(
                                note = note,
                                decryptedTitle = decryptedPair.first,
                                decryptedContent = decryptedPair.second,
                                isSelected = isSelected,
                                searchQuery = searchQuery.takeIf { it.isNotEmpty() },
                                onClick = { onNoteSelected(note.idNote) }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        } else {
            // Contenu de l'onglet Coffre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Vous êtes dans la vue du coffre d'images",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
    
    // Afficher la boîte de dialogue de confirmation de mot de passe si demandé
    if (showPasswordDialog) {
        PasswordConfirmationDialog(
            currentUser = currentUser,
            onDismiss = { showPasswordDialog = false },
            onAccessGranted = {
                showPasswordDialog = false
                activeTab = "vault"
                onGoToVault()
            }
        )
    }
}

/**
 * Boîte de dialogue de confirmation de mot de passe pour accéder au coffre-fort
 */
@Composable
fun PasswordConfirmationDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onAccessGranted: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingPassword by remember { mutableStateOf(false) }
    
    // Créer une instance du repository utilisateur
    val userRepository = remember { UserRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF2A2A3A)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Authentification requise",
                        style = MaterialTheme.typography.h6,
                        color = Color.White
                    )
                }
                
                // Message explicatif
                Text(
                    text = "Veuillez confirmer votre mot de passe pour accéder au coffre-fort d'images.",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // Champ de mot de passe
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Mot de passe") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            // Utiliser du texte au lieu d'une icône
                            Text(
                                text = if (passwordVisible) "Cacher" else "Voir",
                                color = MaterialTheme.colors.primary
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color.Gray,
                        backgroundColor = Color(0xFF1A1A2E)
                    ),
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Message d'erreur
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Annuler")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isCheckingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colors.primary
                        )
                    } else {
                        Button(
                            onClick = {
                                isCheckingPassword = true
                                
                                // Utiliser authenticateUser de userRepository pour vérifier le mot de passe
                                coroutineScope.launch {
                                    val isPasswordCorrect = withContext(Dispatchers.IO) {
                                        // Utiliser le repository pour authentifier l'utilisateur
                                        val authenticatedUser = userRepository.authenticateUser(
                                            currentUser.userLogin, 
                                            password
                                        )
                                        authenticatedUser != null
                                    }
                                    
                                    isCheckingPassword = false
                                    
                                    if (isPasswordCorrect) {
                                        onAccessGranted()
                                    } else {
                                        errorMessage = "Mot de passe incorrect"
                                    }
                                }
                            },
                            enabled = password.isNotEmpty() && !isCheckingPassword,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Confirmer")
                        }
                    }
                }
            }
        }
    }
}

// Composant de carte de note optimisé avec mise en évidence du texte recherché
@Composable
fun OptimizedNoteCard(
    note: Note,
    decryptedTitle: String,
    decryptedContent: String,
    isSelected: Boolean,
    searchQuery: String? = null,
    onClick: () -> Unit
) {
    // Mémoriser la couleur pour éviter de recalculer à chaque recomposition
    val noteColorValue = remember(note.color.colorName) {
        getColorFromName(note.color.colorName)
    }
    
    // Appliquer une version plus claire (ou désaturée) pour le fond
    val backgroundColor = remember(noteColorValue, isSelected) {
        noteColorValue.copy(alpha = if (isSelected) 0.3f else 0.15f)
    }
    
    // Bordure de la couleur originale si sélectionnée
    val borderColor = remember(noteColorValue, isSelected) {
        if (isSelected) noteColorValue else Color.Transparent
    }
    
    // Détermine si la note a été modifiée
    val isModified = note.noteUpdateDate != null
    
    // Mémoriser la date formatée pour éviter de la reformater à chaque recomposition
    val dateToShow = note.noteUpdateDate ?: note.noteCreationDate
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) } // Ajout des heures et minutes
    val formattedDate = remember(dateToShow) { 
        dateFormat.format(Date.from(dateToShow.atZone(ZoneId.systemDefault()).toInstant()))
    }
    
    // Texte à afficher pour la date
    val dateText = remember(isModified, formattedDate) {
        if (isModified) "Modifié le : $formattedDate" else formattedDate
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Titre de la note
        Text(
            text = decryptedTitle,
            style = MaterialTheme.typography.subtitle1,
            color = if (isSelected) MaterialTheme.colors.primary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Aperçu du contenu
        Text(
            text = decryptedContent,
            style = MaterialTheme.typography.body2,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Date de création/modification
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Afficher le badge de correspondance de recherche si applicable
            if (searchQuery != null && (
                decryptedTitle.contains(searchQuery, ignoreCase = true) || 
                decryptedContent.contains(searchQuery, ignoreCase = true)
            )) {
                Text(
                    text = "Correspond",
                    style = MaterialTheme.typography.caption,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colors.primary.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // Date avec style adapté
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = dateText,
                style = MaterialTheme.typography.caption,
                color = if (isModified) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                modifier = if (isModified) Modifier
                    .background(
                        color = noteColorValue.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                else Modifier
            )
        }
    }
}

// Composant de profil optimisé avec mémoisations
@Composable
fun ProfileSection(currentUser: User, onLogout: () -> Unit) {
    // Mémoriser la première lettre du login pour l'avatar
    val avatarLetter = remember(currentUser.userLogin) {
        currentUser.userLogin.take(1).uppercase()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarLetter,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // User name
            Column {
                Text(
                    text = currentUser.userLogin,
                    color = Color.White
                )
                Text(
                    text = "Connecté",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.caption
                )
            }
        }
        
        // Logout button
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF2A2A3A), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Déconnexion",
                tint = Color.White
            )
        }
    }
}

// Utilitaire pour obtenir une couleur basée sur son nom - inchangé
fun getColorFromName(colorName: String): Color {
    return when (colorName.lowercase()) {
        "rouge" -> Color(0xFFE57373)
        "bleu" -> Color(0xFF64B5F6)
        "vert" -> Color(0xFF81C784)
        "jaune" -> Color(0xFFFFD54F)
        "violet" -> Color(0xFFBA68C8)
        "orange" -> Color(0xFFFFB74D)
        "rose" -> Color(0xFFF06292)
        "bleu foncé" -> Color(0xFF7986CB)
        "vert foncé" -> Color(0xFF4DB6AC)
        "gris" -> Color(0xFF90A4AE)
        else -> Color.Gray
    }
}