package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                                    // Annuler le job de debounce en cours
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
                                
                                // Sélectionner la nouvelle note
                                selectedNote = note
                                val decrypted = decryptedNotes[note.idNote]
                                if (decrypted != null) {
                                    editorTitle = TextFieldValue(decrypted.first)
                                    editorContent = TextFieldValue(decrypted.second)
                                    selectedColor = note.color
                                    
                                    // Initialiser les variables de suivi des modifications
                                    lastSavedTitle = decrypted.first
                                    lastSavedContent = decrypted.second
                                    noteModified = false
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

                                                // Annuler le job de debounce en cours si présent
                                                debounceJob?.cancel()

                                                // Sélectionner automatiquement la nouvelle note
                                                println("DEBUG: Sélection de la nouvelle note")
                                                selectedNote = newNote
                                                editorTitle = TextFieldValue("Nouvelle note")
                                                editorContent = TextFieldValue("")
                                                selectedColor = availableColors.find { it.idColor == 6 }  // Facultatif : pour afficher la bonne couleur dans l'UI
                                                
                                                // Initialiser les variables de suivi des modifications pour la nouvelle note
                                                lastSavedTitle = "Nouvelle note"
                                                lastSavedContent = ""
                                                noteModified = false

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
                                                debounceJob?.cancel()
                                                noteModified = false
                                            }
                                        }
                                    }
                                }
                            },
                            onLogout = {
                                // Annuler le job de debounce en cours
                                debounceJob?.cancel()
                                
                                if (selectedNote != null && noteModified) {
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
                                noteColor = selectedColor,
                                availableColors = availableColors,
                                onColorChange = { newColor ->
                                    selectedColor = newColor
                                    
                                    // Sauvegarder le changement de couleur en arrière-plan
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            noteRepository.updateNote(
                                                noteId = selectedNote!!.idNote,
                                                colorId = newColor.idColor
                                            )
                                            
                                            // Rafraîchir les notes pour mettre à jour l'affichage dans la barre latérale
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