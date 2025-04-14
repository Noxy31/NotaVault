package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.entities.Note
import org.example.entities.User
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.ZoneOffset

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
    onOpenVault: () -> Unit,
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
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                // Bouton Vault - coffre-fort d'images
                Button(
                    onClick = onOpenVault,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF4B4B63)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Coffre d'images",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Vault",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Bouton Ajouter note (plus petit)
                IconButton(
                    onClick = onNewNote,
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colors.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouvelle note",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
    // Création d'un formateur avec le fuseau horaire local
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    
    // Convertir la date UTC de la base de données au fuseau horaire local
    val noteDate = note.noteUpdateDate ?: note.noteCreationDate
    val localDateTime = noteDate.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    val formattedDate = localDateTime.format(dateFormatter)
    
    // Obtenir la couleur de la note avec une opacité réduite pour le fond
    val noteColor = getColorFromName(note.color.colorName).copy(alpha = 0.25f)
    val textColor = Color.White
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        backgroundColor = noteColor,
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = decryptedTitle.ifEmpty { "Sans titre" },
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = decryptedContent.take(50).ifEmpty { "Pas de contenu" } + 
                        if (decryptedContent.length > 50) "..." else "",
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formattedDate,
                    color = textColor.copy(alpha = 0.5f),
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
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }
    } 
}