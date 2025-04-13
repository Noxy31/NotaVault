package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.example.entities.Note
import org.example.entities.User
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes

@Composable
fun MainScreen(
    currentUser: User,
    onLogout: () -> Unit
) {
    val noteRepository = remember { NoteRepository().apply { setEncryptionKey(currentUser) } }
    val coroutineScope = rememberCoroutineScope()
    
    // État pour stocker les notes de l'utilisateur
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    
    // État pour stocker les notes décryptées (titre, contenu)
    var decryptedNotes by remember { mutableStateOf<Map<Int, Pair<String, String>>>(emptyMap()) }
    
    // Note actuellement sélectionnée
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    
    // États pour le titre et le contenu de l'éditeur
    var editorTitle by remember { mutableStateOf(TextFieldValue("")) }
    var editorContent by remember { mutableStateOf(TextFieldValue("")) }
    
    // État pour le terme de recherche
    var searchQuery by remember { mutableStateOf("") }
    
    // État pour le job de sauvegarde automatique
    var autoSaveJob by remember { mutableStateOf<Job?>(null) }
    
    // Obtenir toutes les couleurs disponibles
    var availableColors by remember { mutableStateOf(emptyList<org.example.entities.Color>()) }
    
    // Couleur actuellement sélectionnée pour la note
    var selectedColor by remember { mutableStateOf<org.example.entities.Color?>(null) }
    
    // État pour indiquer si une sauvegarde est en cours
    var isSaving by remember { mutableStateOf(false) }
    
    // État pour afficher un message de statut
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // État pour montrer ou masquer le panneau latéral sur mobile
    var showSidebar by remember { mutableStateOf(true) }
    
    // Fonction pour rafraîchir les notes
    fun refreshNotes() {
    println("DEBUG: Début de refreshNotes()")
    coroutineScope.launch {
        try {
            withContext(Dispatchers.IO) {
                println("DEBUG: Récupération des notes de l'utilisateur ${currentUser.idUser}")
                val userNotes = noteRepository.getUserNotes(currentUser.idUser)
                println("DEBUG: ${userNotes.size} notes récupérées")
                
                // Déchiffrer chaque note, avec gestion des erreurs
                val decrypted = mutableMapOf<Int, Pair<String, String>>()
                
                userNotes.forEach { note ->
                    try {
                        println("DEBUG: Déchiffrement de la note ${note.idNote}")
                        val decryptedPair = noteRepository.decryptNote(note) ?: Pair("", "")
                        decrypted[note.idNote] = decryptedPair
                    } catch (e: Exception) {
                        println("DEBUG: Erreur lors du déchiffrement de la note ${note.idNote}: ${e.message}")
                        // En cas d'erreur, mettre des valeurs vides
                        decrypted[note.idNote] = Pair("Erreur de déchiffrement", "")
                    }
                }
                
                println("DEBUG: ${decrypted.size} notes déchiffrées")
                notes = userNotes
                decryptedNotes = decrypted
            }
        } catch (e: Exception) {
            println("DEBUG: EXCEPTION dans refreshNotes: ${e.message}")
            e.printStackTrace()
            // Afficher un message d'erreur
            statusMessage = "Erreur lors du chargement des notes: ${e.message}"
            coroutineScope.launch {
                delay(3000)
                statusMessage = null
            }
        }
    }
}
    
    // Fonction pour sauvegarder la note actuelle
    fun saveCurrentNote(note: Note, title: String, content: String, onComplete: () -> Unit = {}) {
        if (title.isBlank() && content.isBlank()) {
            onComplete()
            return
        }
        
        isSaving = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val success = noteRepository.updateNote(
                    noteId = note.idNote,
                    title = title,
                    content = content
                )
                
                if (success) {
                    refreshNotes()
                }
            }
            isSaving = false
            onComplete()
        }
    }
    
    // Charger les notes et les couleurs au démarrage
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                availableColors = noteRepository.getAvailableColors()
                refreshNotes()
            }
        }
    }
    
    // Définir le thème sombre personnalisé
    ModernDarkTheme {
        // Structure principale avec Box pour le fond d'écran dégradé
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
            // Contenu principal
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = maxWidth
                val isTabletOrDesktop = width > 700.dp
                
                Row(modifier = Modifier.fillMaxSize()) {
                    // Barre latérale (menu)
                    AnimatedVisibility(
                        visible = showSidebar || isTabletOrDesktop,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        SidebarPanel(
                            width = if (isTabletOrDesktop) 300.dp else width * 0.8f,
                            notes = notes,
                            decryptedNotes = decryptedNotes,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { newQuery ->
                                searchQuery = newQuery
                                coroutineScope.launch {
                                    if (newQuery.isBlank()) {
                                        refreshNotes()
                                    } else {
                                        withContext(Dispatchers.IO) {
                                            val searchResults = noteRepository.searchNotes(currentUser.idUser, newQuery)
                                            val decryptedResults = searchResults.associateBy(
                                                { it.idNote },
                                                { noteRepository.decryptNote(it) ?: Pair("", "") }
                                            )
                                            
                                            notes = searchResults
                                            decryptedNotes = decryptedResults
                                        }
                                    }
                                }
                            },
                            onNoteSelected = { note ->
                                // Sauvegarder les modifications de la note actuellement ouverte
                                if (selectedNote != null && (selectedNote!!.idNote != note.idNote)) {
                                    saveCurrentNote(selectedNote!!, editorTitle.text, editorContent.text) {
                                        statusMessage = "Note sauvegardée"
                                        coroutineScope.launch {
                                            delay(2000)
                                            statusMessage = null
                                        }
                                    }
                                }
                                
                                // Sélectionner la nouvelle note
                                selectedNote = note
                                val decrypted = decryptedNotes[note.idNote]
                                if (decrypted != null) {
                                    editorTitle = TextFieldValue(decrypted.first)
                                    editorContent = TextFieldValue(decrypted.second)
                                    selectedColor = note.color
                                }
                                
                                // Démarrer le job de sauvegarde automatique
                                autoSaveJob?.cancel()
                                autoSaveJob = coroutineScope.launch {
                                    while (isActive) {
                                        delay(1.minutes)
                                        saveCurrentNote(note, editorTitle.text, editorContent.text) {
                                            statusMessage = "Auto-sauvegarde effectuée"
                                            coroutineScope.launch {
                                                delay(2000)
                                                statusMessage = null
                                            }
                                        }
                                    }
                                }
                                
                                if (!isTabletOrDesktop) {
                                    showSidebar = false
                                }
                            },
                            onNewNote = {
                                println("DEBUG: Bouton + cliqué, création d'une nouvelle note")
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            // Simplifier la gestion des couleurs en utilisant directement l'ID 6 sans vérification
                                            println("DEBUG: Création d'une note avec couleur ID=6 (violet)")
                                            val newNote = noteRepository.createNote(
                                                userId = currentUser.idUser,
                                                title = "Nouvelle note",
                                                content = "",
                                                colorId = 6  // Toujours utiliser l'ID 6 (violet) pour les nouvelles notes
                                            )

                                            println("DEBUG: Résultat création note: $newNote")

                                            if (newNote != null) {
                                                println("DEBUG: Note créée avec succès, refreshNotes()")
                                                refreshNotes()

                                                // Sélectionner automatiquement la nouvelle note
                                                println("DEBUG: Sélection de la nouvelle note")
                                                selectedNote = newNote
                                                editorTitle = TextFieldValue("Nouvelle note")
                                                editorContent = TextFieldValue("")
                                                selectedColor = availableColors.find { it.idColor == 6 }  // Facultatif : pour afficher la bonne couleur dans l'UI

                                                // Démarrer le job de sauvegarde automatique
                                                println("DEBUG: Configuration de l'auto-save")
                                                autoSaveJob?.cancel()
                                                autoSaveJob = coroutineScope.launch {
                                                    while (isActive) {
                                                        delay(1.minutes)
                                                        saveCurrentNote(newNote, editorTitle.text, editorContent.text) {
                                                            statusMessage = "Auto-sauvegarde effectuée"
                                                            coroutineScope.launch {
                                                                delay(2000)
                                                                statusMessage = null
                                                            }
                                                        }
                                                    }
                                                }

                                                // Fermer le panneau latéral sur mobile
                                                if (!isTabletOrDesktop) {
                                                    showSidebar = false
                                                }

                                                // Afficher un message de confirmation
                                                statusMessage = "Note créée avec succès"
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    statusMessage = null
                                                }
                                            } else {
                                                println("DEBUG: ERREUR - La note créée est null")
                                                statusMessage = "Erreur lors de la création de la note"
                                                coroutineScope.launch {
                                                    delay(2000)
                                                    statusMessage = null
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println("DEBUG: EXCEPTION lors de la création de la note: ${e.message}")
                                            e.printStackTrace()
                                            statusMessage = "Erreur: ${e.message}"
                                            coroutineScope.launch {
                                                delay(3000)
                                                statusMessage = null
                                            }
                                        }
                                    }
                                }
                        
                            },

                            onDeleteNote = { note ->
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (noteRepository.deleteNote(note.idNote)) {
                                            refreshNotes()
                                            
                                            // Si la note supprimée était sélectionnée, désélectionner
                                            if (selectedNote?.idNote == note.idNote) {
                                                selectedNote = null
                                                editorTitle = TextFieldValue("")
                                                editorContent = TextFieldValue("")
                                                autoSaveJob?.cancel()
                                            }
                                        }
                                    }
                                }
                            },
                            onLogout = {
                                autoSaveJob?.cancel()
                                if (selectedNote != null) {
                                    saveCurrentNote(selectedNote!!, editorTitle.text, editorContent.text) {
                                        onLogout()
                                    }
                                } else {
                                    onLogout()
                                }
                            },
                            currentUser = currentUser
                        )
                    }
                    
                    // Contenu principal (éditeur de notes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (!isTabletOrDesktop && !showSidebar) {
                            // Bouton pour afficher le menu latéral sur mobile
                            IconButton(
                                onClick = { showSidebar = true },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(48.dp)
                                    .background(MaterialTheme.colors.primary, CircleShape)
                                    .align(Alignment.TopStart)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        if (selectedNote != null) {
                            NoteEditor(
                                title = editorTitle,
                                onTitleChange = { editorTitle = it },
                                content = editorContent,
                                onContentChange = { editorContent = it },
                                noteColor = selectedColor,
                                availableColors = availableColors,
                                onColorChange = { newColor ->
                                    selectedColor = newColor
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            noteRepository.updateNote(
                                                noteId = selectedNote!!.idNote,
                                                colorId = newColor.idColor
                                            )
                                        }
                                    }
                                },
                                onSave = {
                                    saveCurrentNote(selectedNote!!, editorTitle.text, editorContent.text) {
                                        statusMessage = "Note sauvegardée"
                                        coroutineScope.launch {
                                            delay(2000)
                                            statusMessage = null
                                        }
                                    }
                                },
                                isSaving = isSaving
                            )
                        } else {
                            // Affichage d'un message si aucune note n'est sélectionnée
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        tint = Color.White.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Sélectionnez une note ou créez-en une nouvelle",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        // Affichage du message de statut
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            if (statusMessage != null) {
                                Card(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = 4.dp,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.Center)
                                ) {
                                    Text(
                                        text = statusMessage ?: "",
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panneau latéral avec la liste des notes et la barre de recherche
 */
@Composable
fun SidebarPanel(
    width: androidx.compose.ui.unit.Dp,
    notes: List<Note>,
    decryptedNotes: Map<Int, Pair<String, String>>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNoteSelected: (Note) -> Unit,
    onNewNote: () -> Unit,
    onDeleteNote: (Note) -> Unit,
    onLogout: () -> Unit,
    currentUser: User
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 16.dp)
    ) {
        // En-tête avec le nom de l'application et le bouton de déconnexion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NotaVault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF3A3A3A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Déconnexion",
                    tint = Color.White
                )
            }
        }
        
        // Info utilisateur
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar basé sur les initiales de l'utilisateur
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colors.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.userLogin.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = currentUser.userLogin,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Barre de recherche
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Rechercher...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rechercher",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = MaterialTheme.colors.primary,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.Gray,
                backgroundColor = Color(0xFF2A2A3A)
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        
        // En-tête des notes avec le bouton pour ajouter une nouvelle note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mes notes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            IconButton(
                onClick = onNewNote,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colors.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nouvelle note",
                    tint = Color.White
                )
            }
        }
        
        // Liste des notes
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Aucune note" else "Aucun résultat",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        decryptedTitle = decryptedNotes[note.idNote]?.first ?: "",
                        decryptedContent = decryptedNotes[note.idNote]?.second ?: "",
                        onClick = { onNoteSelected(note) },
                        onDelete = { onDeleteNote(note) }
                    )
                }
            }
        }
    }
}

/**
 * Élément représentant une note dans la liste
 */
@Composable
fun NoteItem(
    note: Note,
    decryptedTitle: String,
    decryptedContent: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val formattedDate = note.noteUpdateDate?.format(dateFormatter) ?: note.noteCreationDate.format(dateFormatter)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        backgroundColor = Color(0xFF2A2A3A),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicateur de couleur
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        getColorFromName(note.color.colorName),
                        CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = decryptedTitle.ifEmpty { "Sans titre" },
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = decryptedContent.take(50).ifEmpty { "Pas de contenu" } + 
                        if (decryptedContent.length > 50) "..." else "",
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = formattedDate,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Éditeur de note
 */
@Composable
fun NoteEditor(
    title: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    noteColor: org.example.entities.Color?,
    availableColors: List<org.example.entities.Color>,
    onColorChange: (org.example.entities.Color) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête de l'éditeur
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sélecteur de couleur
            Card(
                modifier = Modifier
                    .padding(end = 8.dp),
                backgroundColor = Color(0xFF2A2A3A),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Couleur:",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableColors.forEach { color ->
                            val isSelected = noteColor?.idColor == color.idColor
                            
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(getColorFromName(color.colorName))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorChange(color) }
                            )
                        }
                    }
                }
            }
            
            // Bouton de sauvegarde
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "💾",  // Emoji disque
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sauvegarder")
                }
            }
        }
        
        // Champ pour le titre
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Titre de la note") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = MaterialTheme.colors.primary,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.Gray,
                backgroundColor = Color(0xFF2A2A3A)
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        
        // Champ pour le contenu
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text("Contenu de la note") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                cursorColor = MaterialTheme.colors.primary,
                focusedBorderColor = MaterialTheme.colors.primary,
                unfocusedBorderColor = Color.Gray,
                backgroundColor = Color(0xFF2A2A3A)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        // Mettre le focus sur le champ de contenu si le titre est déjà rempli
        LaunchedEffect(Unit) {
            if (title.text.isNotEmpty()) {
                focusRequester.requestFocus()
            }
        }
    }
}

/**
 * Fonction utilitaire pour obtenir la couleur à partir du nom et/ou de la valeur hexadécimale
 */
fun getColorFromName(colorName: String, colorHexa: String? = null): Color {
    // Si on a fourni une valeur hexadécimale, l'utiliser directement
    if (colorHexa != null && colorHexa.isNotEmpty()) {
        try {
            // Pour être compatible avec la fonction de parsing de couleur d'Android et de Compose
            val hexCode = colorHexa.replace("#", "").trim()
            
            // Vérifier que c'est un code hexa valide
            if (hexCode.matches(Regex("[0-9A-Fa-f]{6}"))) {
                val r = hexCode.substring(0, 2).toInt(16)
                val g = hexCode.substring(2, 4).toInt(16)
                val b = hexCode.substring(4, 6).toInt(16)
                return Color(r, g, b, 255) // Alpha à 255 (opaque)
            }
        } catch (e: Exception) {
            println("DEBUG: Erreur lors du parsing de la couleur hexa: $colorHexa - ${e.message}")
        }
    }
    
    // Fallback sur les valeurs codées en dur
    return when (colorName.lowercase()) {
        "rouge" -> Color(0xFFFF5252)
        "bleu" -> Color(0xFF536DFE)
        "vert" -> Color(0xFF4CAF50)
        "jaune" -> Color(0xFFFFEB3B)
        "orange" -> Color(0xFFFF9800)
        "violet" -> Color(0xFF9C27B0)
        "rose" -> Color(0xFFE91E63)
        "turquoise" -> Color(0xFF00BCD4)
        "gris" -> Color(0xFF9E9E9E)
        else -> Color(0xFF9D72FF) // Couleur primaire par défaut
    }
}

/**
 * Fonction utilitaire pour obtenir la couleur hexadécimale à partir du nom de couleur
 */
fun getColorHexFromName(colorName: String): String {
    return when (colorName.lowercase()) {
        "rouge" -> "#FF5252"
        "bleu" -> "#536DFE"
        "vert" -> "#4CAF50"
        "jaune" -> "#FFEB3B"
        "orange" -> "#FF9800"
        "violet" -> "#9C27B0"
        "rose" -> "#E91E63"
        "turquoise" -> "#00BCD4"
        "gris" -> "#9E9E9E"
        else -> "#9D72FF" // Couleur primaire par défaut
    }
}