package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import org.example.entities.Note
import org.example.entities.User
import kotlin.time.Duration.Companion.seconds

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
    
    // Pour suivre si des modifications ont été apportées depuis la dernière sauvegarde
    var noteModified by remember { mutableStateOf(false) }
    
    // Pour stocker le dernier contenu sauvegardé
    var lastSavedTitle by remember { mutableStateOf("") }
    var lastSavedContent by remember { mutableStateOf("") }
    
    // État pour le terme de recherche
    var searchQuery by remember { mutableStateOf("") }
    
    // État pour le job de sauvegarde automatique avec debounce
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    
    // Obtenir toutes les couleurs disponibles
    var availableColors by remember { mutableStateOf(emptyList<org.example.entities.Color>()) }
    
    // *** IMPORTANT: Séparer clairement la couleur de la note et le filtre de couleur ***
    // Filtre de couleur pour la sidebar - UNIQUEMENT modifié par la sidebar
    var colorFilter by remember { mutableStateOf<org.example.entities.Color?>(null) }
    
    // État local pour la couleur active de la note (pour mise à jour immédiate de l'UI)
    var activeNoteColor by remember { mutableStateOf<org.example.entities.Color?>(null) }
    
    // État pour indiquer si une sauvegarde est en cours
    var isSaving by remember { mutableStateOf(false) }
    
    // État pour afficher un message de statut
    var statusMessage by remember { mutableStateOf<String?>(null) }
    
    // État pour montrer ou masquer le panneau latéral sur mobile
    var showSidebar by remember { mutableStateOf(true) }
    
    // État pour savoir si on affiche le coffre d'images
    var showVault by remember { mutableStateOf(false) }
    
    // Fonction pour rafraîchir les notes
    fun refreshNotes() {
        coroutineScope.launch {
            try {
                val userNotes = withContext(Dispatchers.IO) {
                    if (colorFilter != null) {
                        noteRepository.getNotesByColor(currentUser.idUser, colorFilter!!.idColor)
                    } else {
                        noteRepository.getUserNotes(currentUser.idUser)
                    }
                }
                
                // Déchiffrer en batch dans un thread d'arrière-plan
                val decrypted = withContext(Dispatchers.IO) {
                    noteRepository.decryptNotesInBatch(userNotes)
                }
                
                // Mettre à jour les états en une seule fois
                notes = userNotes
                decryptedNotes = decrypted
                
                // Mise à jour de la note sélectionnée si nécessaire
                if (selectedNote != null) {
                    val updatedNote = userNotes.find { it.idNote == selectedNote!!.idNote }
                    if (updatedNote != null) {
                        selectedNote = updatedNote
                        activeNoteColor = updatedNote.color
                    }
                }
            } catch (e: Exception) {
                println("EXCEPTION dans refreshNotes: ${e.message}")
                e.printStackTrace()
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
            
            // Mettre à jour les variables de suivi après la sauvegarde
            lastSavedTitle = title
            lastSavedContent = content
            noteModified = false
            
            onComplete()
        }
    }
    
    // Fonction pour déclencher une sauvegarde avec debounce
    fun triggerDebounceAutosave() {
        if (selectedNote == null || !noteModified) return
        
        // Annuler le job précédent si toujours actif
        debounceJob?.cancel()
        
        // Créer un nouveau job qui attendra 3 secondes avant de sauvegarder
        debounceJob = coroutineScope.launch {
            delay(3.seconds)
            saveCurrentNote(selectedNote!!, editorTitle.text, editorContent.text) {
                statusMessage = "Auto-sauvegarde effectuée"
                coroutineScope.launch {
                    delay(2000)
                    statusMessage = null
                }
            }
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
                
                if (showVault) {
                    // Afficher le VaultScreen si activé
                    VaultScreen(
                        currentUser = currentUser,
                        onBackToNotes = { showVault = false }
                    )
                } else {
                    // Afficher l'écran principal des notes
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Barre latérale (menu)
                        AnimatedVisibility(
                            visible = showSidebar || isTabletOrDesktop,
                            enter = slideInHorizontally() + fadeIn(),
                            exit = slideOutHorizontally() + fadeOut()
                        ) {
                            SidebarComponent(
                                currentUser = currentUser,
                                notes = notes,
                                selectedNoteId = selectedNote?.idNote,
                                onNoteSelected = { noteId ->
                                    val note = notes.find { it.idNote == noteId }
                                    if (note != null) {
                                        // Sauvegarder les modifications de la note actuellement ouverte
                                        if (selectedNote != null && (selectedNote!!.idNote != note.idNote)) {
                                            debounceJob?.cancel()
                                            if (noteModified) {
                                                saveCurrentNote(selectedNote!!, editorTitle.text, editorContent.text) {
                                                    statusMessage = "Note sauvegardée"
                                                    coroutineScope.launch {
                                                        delay(2000)
                                                        statusMessage = null
                                                    }
                                                }
                                            }
                                        }
                                        
                                        selectedNote = note
                                        // Mettre à jour la couleur active immédiatement
                                        activeNoteColor = note.color
                                        
                                        val decrypted = decryptedNotes[note.idNote]
                                        if (decrypted != null) {
                                            editorTitle = TextFieldValue(decrypted.first)
                                            editorContent = TextFieldValue(decrypted.second)
                                            lastSavedTitle = decrypted.first
                                            lastSavedContent = decrypted.second
                                            noteModified = false
                                        }
                                        
                                        if (!isTabletOrDesktop) {
                                            showSidebar = false
                                        }
                                    }
                                },
                               onAddNote = {
                                coroutineScope.launch {
                                    // Créer la note avec des valeurs initiales
                                    val newNote = withContext(Dispatchers.IO) {
                                        noteRepository.createNote(
                                            userId = currentUser.idUser,
                                            title = "Nouvelle note",
                                            content = "",
                                            colorId = 6
                                        )
                                    }
                                    
                                    if (newNote != null) {
                                        // Rafraîchir complètement la liste des notes
                                        refreshNotes()
                                        
                                        // Effectuer la même séquence d'actions que lorsqu'on clique sur une note
                                        // C'est la clé: simuler exactement ce qui se passe quand on clique sur une note
                                        val note = notes.find { it.idNote == newNote.idNote }
                                        if (note != null) {
                                            selectedNote = note
                                            activeNoteColor = note.color
                                            
                                            val decrypted = decryptedNotes[note.idNote]
                                            if (decrypted != null) {
                                                editorTitle = TextFieldValue(decrypted.first)
                                                editorContent = TextFieldValue(decrypted.second)
                                                lastSavedTitle = decrypted.first
                                                lastSavedContent = decrypted.second
                                                noteModified = false
                                            }
                                            
                                            if (!isTabletOrDesktop) {
                                                showSidebar = false
                                            }
                                        }
                                    }
                                }
                            },
                                onGoToVault = {
                                    // Passer à l'écran du coffre d'images
                                    showVault = true
                                },
                                onLogout = onLogout,
                                onColorFilterChange = { color ->
                                    // IMPORTANT: C'est ici que le filtre de couleur est modifié
                                    // Cette fonction est uniquement appelée depuis la sidebar
                                    colorFilter = color
                                    refreshNotes()
                                },
                                selectedColorFilter = colorFilter, // Passer le filtre de couleur à la sidebar
                                colors = availableColors,
                                noteRepository = noteRepository
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
                                    onTitleChange = { 
                                        editorTitle = it 
                                        // Vérifier s'il y a eu une modification
                                        val wasModified = noteModified
                                        noteModified = editorTitle.text != lastSavedTitle || editorContent.text != lastSavedContent
                                        
                                        // Si l'état est passé de non-modifié à modifié, déclencher un debounce
                                        if (noteModified && !wasModified) {
                                            println("DEBUG: Contenu modifié, préparation de l'auto-sauvegarde")
                                        }
                                        
                                        // Déclencher le debounce à chaque modification
                                        if (noteModified) {
                                            triggerDebounceAutosave()
                                        }
                                    },
                                    content = editorContent,
                                    onContentChange = { 
                                        editorContent = it 
                                        // Vérifier s'il y a eu une modification
                                        val wasModified = noteModified
                                        noteModified = editorTitle.text != lastSavedTitle || editorContent.text != lastSavedContent
                                        
                                        // Si l'état est passé de non-modifié à modifié, déclencher un debounce
                                        if (noteModified && !wasModified) {
                                            println("DEBUG: Contenu modifié, préparation de l'auto-sauvegarde")
                                        }
                                        
                                        // Déclencher le debounce à chaque modification
                                        if (noteModified) {
                                            triggerDebounceAutosave()
                                        }
                                    },
                                    noteColor = activeNoteColor, // Utiliser activeNoteColor au lieu de selectedNote?.color
                                    availableColors = availableColors,
                                    onColorChange = { newColor ->
                                        // Mettre à jour immédiatement l'état local pour une UI réactive
                                        activeNoteColor = newColor
                                        
                                        // Sauvegarder le changement de couleur en arrière-plan
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                noteRepository.updateNote(
                                                    noteId = selectedNote!!.idNote,
                                                    colorId = newColor.idColor
                                                )
                                                
                                                // Si la note est actuellement filtrée par couleur
                                                // et que la nouvelle couleur ne correspond pas au filtre,
                                                // désactiver le filtre pour que la note reste visible
                                                if (colorFilter != null && colorFilter!!.idColor != newColor.idColor) {
                                                    colorFilter = null
                                                }
                                                
                                                // Récupérer la note mise à jour
                                                val updatedNote = noteRepository.getNoteById(selectedNote!!.idNote)
                                                if (updatedNote != null) {
                                                    // Mettre à jour l'état en toute sécurité
                                                    withContext(Dispatchers.Default) {
                                                        selectedNote = updatedNote
                                                    }
                                                }
                                                
                                                // Rafraîchir les notes pour mettre à jour l'affichage
                                                refreshNotes()
                                            }
                                        }
                                    },
                                    onSave = {
                                        // Annuler tout debounce en cours
                                        debounceJob?.cancel()
                                        
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
}