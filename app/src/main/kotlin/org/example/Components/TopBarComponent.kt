package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.entities.Album
import org.example.entities.Color as EntityColor
import org.example.entities.Image
import org.example.entities.User

/**
 * Helper function to parse color
 */
fun parseColor(colorString: String): Long {
    val colorStr = colorString.replace("#", "")
    return colorStr.toLong(16) or 0xFF000000
}

/**
 * Composant pour la barre supérieure de gestion des albums
 */
@Composable
fun TopBarComponent(
    currentUser: User,
    vaultImages: List<Image>,
    selectedImages: List<Image>,
    onImagesSelected: (List<Image>) -> Unit,
    onAlbumCreated: () -> Unit,
    onAlbumSelected: (Album?) -> Unit,
    onBackToNotes: () -> Unit,
    onAddImageClick: () -> Unit,
    currentAlbum: Album?
) {
    val albumRepository = remember { AlbumRepository() }
    val colorRepository = remember { ColorRepository() }
    val imageRepository = remember { ImageRepository().apply { setEncryptionKey(currentUser) } }
    val coroutineScope = rememberCoroutineScope()
    
    // États pour les albums et les couleurs
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var availableColors by remember { mutableStateOf<List<EntityColor>>(emptyList()) }
    
    // États pour les dialogues
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var showMoveToAlbumDialog by remember { mutableStateOf(false) }
    var showManageAlbumsDialog by remember { mutableStateOf(false) }
    
    // État pour le mode sélection
    var selectionMode by remember { mutableStateOf(false) }
    
    // Nouveaux états pour le dialogue de création d'album
    var newAlbumName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<EntityColor?>(null) }
    
    // Fonction pour charger les albums
    fun loadAlbums() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                albums = albumRepository.getUserAlbums(currentUser.idUser)
            }
        }
    }
    
    // Fonction pour charger les couleurs
    fun loadColors() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                availableColors = colorRepository.getAllColors()
            }
        }
    }
    
    // Charger les albums et les couleurs au démarrage
    LaunchedEffect(Unit) {
        loadAlbums()
        loadColors()
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Barre d'en-tête principale avec navigation et titre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton retour
            IconButton(
                onClick = onBackToNotes,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A2A3A), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour aux notes",
                    tint = Color.White
                )
            }
            
            // Titre
            Text(
                text = if (currentAlbum != null) "Album: ${currentAlbum.albumName}" else "Coffre d'images",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Boutons d'action
            Row {
                Spacer(modifier = Modifier.width(8.dp))
                
                // Bouton de gestion des albums
                IconButton(
                    onClick = { showManageAlbumsDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2A2A3A), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gérer les albums",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Bouton d'ajout d'image - géré par VaultScreen
                IconButton(
                    onClick = onAddImageClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colors.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter une image",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Barre de filtrage par albums
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Item "Tous" pour afficher toutes les images
            item {
                AlbumChip(
                    album = null,
                    isSelected = currentAlbum == null,
                    onClick = { onAlbumSelected(null) }
                )
            }
            
            // Liste des albums
            items(albums) { album ->
                AlbumChip(
                    album = album,
                    isSelected = currentAlbum?.idAlbum == album.idAlbum,
                    onClick = { onAlbumSelected(album) }
                )
            }
            
            // Bouton pour créer un nouvel album
            item {
                OutlinedButton(
                    onClick = { showCreateAlbumDialog = true },
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MaterialTheme.colors.primary
                    ),
                    border = ButtonDefaults.outlinedBorder
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nouvel album",
                        color = MaterialTheme.colors.primary,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Barre d'action de sélection (visible uniquement en mode sélection)
        if (selectionMode && selectedImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A3A))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre d'images sélectionnées
                Text(
                    text = "${selectedImages.size} sélectionnée(s)",
                    color = Color.White,
                    fontSize = 16.sp
                )
                
                // Boutons d'action sur la sélection
                Row {
                    // Bouton pour déplacer vers un album
                    TextButton(
                        onClick = { showMoveToAlbumDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            // Utiliser une icône disponible
                            imageVector = Icons.Default.Create,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Déplacer")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Bouton pour supprimer de l'album (uniquement si on est dans un album)
                    if (currentAlbum != null) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        for (image in selectedImages) {
                                            imageRepository.removeImageFromAlbum(image.idImage)
                                        }
                                    }
                                    // Désélectionner toutes les images
                                    onImagesSelected(emptyList())
                                    // Désactiver le mode sélection
                                    selectionMode = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF44336))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retirer de l'album",
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogue de création d'album
    if (showCreateAlbumDialog) {
        Dialog(onDismissRequest = { showCreateAlbumDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFF2A2A3A)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Créer un nouvel album",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Champ pour le nom de l'album
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text("Nom de l'album") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            cursorColor = MaterialTheme.colors.primary,
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color.Gray,
                            backgroundColor = Color(0xFF1A1A2E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sélection de couleur
                    Text(
                        text = "Couleur de l'album",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableColors) { color ->
                            ColorItem(
                                color = color,
                                isSelected = selectedColor?.idColor == color.idColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showCreateAlbumDialog = false }
                        ) {
                            Text(
                                text = "Annuler",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (newAlbumName.isNotBlank() && selectedColor != null) {
                                    coroutineScope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                val albumId = albumRepository.createAlbum(
                                                    albumName = newAlbumName, 
                                                    colorId = selectedColor!!.idColor,
                                                    userId = currentUser.idUser
                                                )
                                                
                                                if (albumId != null) {
                                                    // Succès
                                                    // Recharger les albums
                                                    loadAlbums()
                                                    // Notifier la création d'un album
                                                    onAlbumCreated()
                                                } else {
                                                    // Échec de la création
                                                    println("Impossible de créer l'album")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println("Erreur lors de la création de l'album: ${e.message}")
                                            e.printStackTrace()
                                        }
                                        
                                        // Réinitialiser les champs
                                        newAlbumName = ""
                                        selectedColor = null
                                        showCreateAlbumDialog = false
                                    }
                                }
                            },
                            enabled = newAlbumName.isNotBlank() && selectedColor != null,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.primary,
                                disabledBackgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "Créer",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogue pour déplacer des images vers un album
    if (showMoveToAlbumDialog) {
        Dialog(onDismissRequest = { showMoveToAlbumDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFF2A2A3A)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Déplacer vers un album",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (albums.isEmpty()) {
                        Text(
                            text = "Aucun album disponible. Créez un album d'abord.",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        // Liste des albums disponibles
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Option pour retirer des albums
                            AlbumListItem(
                                album = null,
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            for (image in selectedImages) {
                                                imageRepository.removeImageFromAlbum(image.idImage)
                                            }
                                        }
                                        // Désélectionner toutes les images
                                        onImagesSelected(emptyList())
                                        // Désactiver le mode sélection
                                        selectionMode = false
                                        showMoveToAlbumDialog = false
                                    }
                                }
                            )
                            
                            Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                            
                            // Liste des albums
                            for (album in albums) {
                                AlbumListItem(
                                    album = album,
                                    onClick = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                for (image in selectedImages) {
                                                    imageRepository.assignImageToAlbum(image.idImage, album.idAlbum)
                                                }
                                            }
                                            // Désélectionner toutes les images
                                            onImagesSelected(emptyList())
                                            // Désactiver le mode sélection
                                            selectionMode = false
                                            showMoveToAlbumDialog = false
                                        }
                                    }
                                )
                                Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showMoveToAlbumDialog = false }
                        ) {
                            Text(
                                text = "Annuler",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogue de gestion des albums
    if (showManageAlbumsDialog) {
        Dialog(onDismissRequest = { showManageAlbumsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFF2A2A3A)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Gérer les albums",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (albums.isEmpty()) {
                        Text(
                            text = "Aucun album disponible.",
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        // Liste des albums avec options de modification/suppression
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            for (album in albums) {
                                ManageAlbumListItem(
                                    album = album,
                                    onRename = { /* Implémenter la logique de renommage si nécessaire */ },
                                    onDelete = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                albumRepository.deleteAlbum(album.idAlbum)
                                            }
                                            loadAlbums()
                                        }
                                    }
                                )
                                Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                showManageAlbumsDialog = false
                                showCreateAlbumDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nouvel album",
                                color = Color.White
                            )
                        }
                        
                        TextButton(
                            onClick = { showManageAlbumsDialog = false }
                        ) {
                            Text(
                                text = "Fermer",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composant pour afficher un album sous forme de puce dans la barre supérieure
 */
@Composable
fun AlbumChip(
    album: Album?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        if (album != null) {
            // Utiliser directement la valeur hexadécimale sans passer par android.graphics.Color
            Color(parseColor(album.color.colorHexa))
        } else {
            MaterialTheme.colors.primary
        }
    } else {
        Color(0xFF2A2A3A)
    }
    
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = album?.albumName ?: "Tous",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Composant pour afficher un élément de couleur pour la sélection
 */
@Composable
fun ColorItem(
    color: EntityColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(parseColor(color.colorHexa)))
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/**
 * Composant pour afficher un album dans la liste de sélection
 */
@Composable
fun AlbumListItem(
    album: Album?,
    onClick: () -> Unit
) {
    val albumColor = album?.let {
        Color(parseColor(it.color.colorHexa))
    } ?: Color(0xFF757575)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicateur de couleur
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(albumColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Nom de l'album
        Text(
            text = album?.albumName ?: "Aucun album (retirer de l'album)",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

/**
 * Composant pour afficher un album dans la liste de gestion avec options
 */
@Composable
fun ManageAlbumListItem(
    album: Album,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Partie gauche avec couleur et nom
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Indicateur de couleur
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(parseColor(album.color.colorHexa)))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Nom de l'album
            Text(
                text = album.albumName,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
        
        // Bouton d'options
        Box {
            IconButton(onClick = { showDropdownMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White
                )
            }
            
            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false },
                modifier = Modifier.background(Color(0xFF2A2A3A))
            ) {
                DropdownMenuItem(onClick = {
                    onRename()
                    showDropdownMenu = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Renommer",
                        color = Color.White
                    )
                }
                
                DropdownMenuItem(onClick = {
                    onDelete()
                    showDropdownMenu = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supprimer",
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}