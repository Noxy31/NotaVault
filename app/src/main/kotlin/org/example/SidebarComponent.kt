package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onColorFilterChange: (NoteColor?) -> Unit,
    selectedColorFilter: NoteColor?,  // Ceci devrait être null par défaut dans MainScreen
    colors: List<NoteColor>,
    noteRepository: NoteRepository
) {
    var showColorPicker by remember { mutableStateOf(false) }
    
    // État pour contrôler l'onglet actif (Notes ou Coffre)
    var activeTab by remember { mutableStateOf("notes") }
    
    // Assure que le filtre est initialement sur "Toutes les couleurs"
    LaunchedEffect(Unit) {
        if (selectedColorFilter != null) {
            onColorFilterChange(null)
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
        ProfileSection(currentUser, onLogout)
        
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
                        imageVector = Icons.Default.List, // Utilisation de List qui est disponible
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
            
            // Onglet Coffre
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
                        activeTab = "vault"
                        onGoToVault() 
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star, // Utilisation de Star qui est disponible
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
                onClick = onAddNote,
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Color filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { showColorPicker = !showColorPicker }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedColorFilter?.colorName ?: "Toutes les couleurs",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (showColorPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Color picker dropdown
            if (showColorPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .background(Color(0xFF2A2A3A), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    // Option pour montrer toutes les couleurs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onColorFilterChange(null)
                                showColorPicker = false
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Toutes les couleurs",
                            color = Color.White
                        )
                    }
                    
                    // Options pour chaque couleur
                    colors.forEach { color ->
                        val isSelected = selectedColorFilter?.idColor == color.idColor
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorFilterChange(color)
                                    showColorPicker = false
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(getColorFromName(color.colorName), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = color.colorName,
                                color = Color.White
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Mes notes",
                    style = MaterialTheme.typography.h6,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                notes.forEach { note ->
                    val isSelected = note.idNote == selectedNoteId
                    val (decryptedTitle, decryptedContent) = try {
                        noteRepository.decryptNote(note) ?: Pair("", "")
                    } catch (e: Exception) {
                        Pair("Erreur de déchiffrement", "")
                    }
                    
                    NoteCard(
                        note = note,
                        decryptedTitle = decryptedTitle,
                        decryptedContent = decryptedContent,
                        isSelected = isSelected,
                        onClick = { onNoteSelected(note.idNote) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        } else {
            // Contenu de l'onglet Coffre (simplement un message informant que le coffre est accessible)
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
                        imageVector = Icons.Default.Star, // Utilisation de Star à nouveau
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
}

@Composable
fun NoteCard(
    note: Note,
    decryptedTitle: String,
    decryptedContent: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Obtenir la couleur de fond basée sur la couleur de la note
    val noteColorValue = getColorFromName(note.color.colorName)
    
    // Appliquer une version plus claire (ou désaturée) pour le fond
    val backgroundColor = noteColorValue.copy(alpha = if (isSelected) 0.3f else 0.15f)
    
    // Bordure de la couleur originale si sélectionnée
    val borderColor = if (isSelected) noteColorValue else Color.Transparent
    
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Affichage de la date
            val dateToShow = note.noteUpdateDate ?: note.noteCreationDate
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
            val formattedDate = remember(dateToShow) { 
                dateFormat.format(Date.from(dateToShow.atZone(ZoneId.systemDefault()).toInstant()))
            }
            
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.caption,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ProfileSection(currentUser: User, onLogout: () -> Unit) {
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
                    text = currentUser.userLogin.take(1).uppercase(),
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

// Utilitaire pour obtenir une couleur basée sur son nom
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