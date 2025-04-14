package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.entities.Color as NoteColor

/**
 * Éditeur de note
 */
@Composable
fun NoteEditor(
    title: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    noteColor: NoteColor?,
    availableColors: List<NoteColor>,
    onColorChange: (NoteColor) -> Unit,
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
            // Sélecteur de couleur amélioré
            Card(
                modifier = Modifier
                    .padding(end = 8.dp),
                backgroundColor = Color(0xFF2A2A3A),
                shape = RoundedCornerShape(8.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Titre du sélecteur
                    Text(
                        text = "Couleur:",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Affichage de la palette de couleurs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        availableColors.forEach { color ->
                            val isSelected = noteColor?.idColor == color.idColor
                            val displayColor = getColorFromName(color.colorName)
                            
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onColorChange(color) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sélectionné",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
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
        
        // Zone d'édition avec un fond légèrement coloré
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = noteColor?.let { 
                        getColorFromName(it.colorName).copy(alpha = 0.1f) 
                    } ?: Color(0xFF2A2A3A).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column {
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
                        focusedBorderColor = noteColor?.let { getColorFromName(it.colorName) } 
                            ?: MaterialTheme.colors.primary,
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
                        focusedBorderColor = noteColor?.let { getColorFromName(it.colorName) } 
                            ?: MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color.Gray,
                        backgroundColor = Color(0xFF2A2A3A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        // Mettre le focus sur le champ de contenu si le titre est déjà rempli
        LaunchedEffect(Unit) {
            if (title.text.isNotEmpty()) {
                focusRequester.requestFocus()
            }
        }
    }
}