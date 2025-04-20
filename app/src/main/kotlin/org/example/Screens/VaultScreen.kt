package org.example

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*
import org.example.entities.Album
import org.example.entities.Image
import org.example.entities.User
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.ByteArrayInputStream
import org.jetbrains.skia.Image as SkiaImage

/**
 * Écran du coffre-fort d'images
 */
@Composable
fun VaultScreen(
    currentUser: User,
    onBackToNotes: () -> Unit
) {
    val imageRepository = remember { ImageRepository().apply { setEncryptionKey(currentUser) } }
    val coroutineScope = rememberCoroutineScope()
    
    // État pour les images du coffre
    var vaultImages by remember { mutableStateOf<List<Image>>(emptyList()) }
    
    // État pour l'image actuellement sélectionnée
    var selectedImage by remember { mutableStateOf<Image?>(null) }
    
    // État pour l'image complète chargée (version déchiffrée)
    var fullImageData by remember { mutableStateOf<ByteArray?>(null) }

    // État pour le dialogue de déplacement vers un album
    var showMoveToAlbumForImageDialog by remember { mutableStateOf(false) }
    var imageToMove by remember { mutableStateOf<Image?>(null) }

    // États pour les dialogues
    var showImageDetail by remember { mutableStateOf(false) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // État pour le chargement
    var isLoading by remember { mutableStateOf(false) }
    
    // Nouveau nom pour le renommage
    var newImageName by remember { mutableStateOf("") }
    
    // Nouvel état pour l'album actuellement sélectionné
    var currentAlbum by remember { mutableStateOf<Album?>(null) }
    
    // Nouvel état pour les images sélectionnées
    var selectedImages by remember { mutableStateOf<List<Image>>(emptyList()) }
    
    // État pour le mode sélection
    var selectionMode by remember { mutableStateOf(false) }
    
    // Fonction pour charger les images
    fun loadImages() {
        isLoading = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                vaultImages = if (currentAlbum != null) {
                    imageRepository.getUserImages(currentUser.idUser, currentAlbum?.idAlbum)
                } else {
                    imageRepository.getUserImages(currentUser.idUser)
                }
            }
            isLoading = false
        }
    }
    
    // Charger les images au démarrage et quand l'album change
    LaunchedEffect(currentAlbum) {
        loadImages()
    }
    
    // Fonction pour obtenir le type MIME d'un fichier
    fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".jpg", ignoreCase = true) || 
            fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
            fileName.endsWith(".bmp", ignoreCase = true) -> "image/bmp"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "application/octet-stream"
        }
    }
    
    // Fonction pour ouvrir le sélecteur de fichiers
    fun selectImageFile(): File? {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Sélectionner une image"
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.fileFilter = FileNameExtensionFilter(
            "Images", "jpg", "jpeg", "png", "gif", "bmp", "webp"
        )
        
        val result = fileChooser.showOpenDialog(null)
        return if (result == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else {
            null
        }
    }
    
    // Thème sombre
    ModernDarkTheme {
        // Structure principale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Intégration de la TopBarComponent
                TopBarComponent(
                    currentUser = currentUser,
                    vaultImages = vaultImages,
                    selectedImages = selectedImages,
                    onAddImageClick = { showAddImageDialog = true },
                    onImagesSelected = { images ->
                        selectedImages = images
                    },
                    onAlbumCreated = {
                        // Recharger les images après la création d'un album
                        loadImages()
                    },
                    onAlbumSelected = { album ->
                        currentAlbum = album
                        selectedImages = emptyList() // Réinitialiser la sélection lors du changement d'album
                    },
                    onBackToNotes = onBackToNotes,
                    currentAlbum = currentAlbum
                )
                
                // Grille d'images
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colors.primary)
                    }
                } else if (vaultImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Simple texte au lieu d'icône pour éviter les problèmes
                            Text(
                                text = "🖼️",
                                fontSize = 64.sp,
                                color = Color.White.copy(alpha = 0.5f),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (currentAlbum != null) "Aucune image dans cet album" else "Aucune image dans le coffre",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showAddImageDialog = true },
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ajouter une image",
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(vaultImages) { image ->
                            ImageCard(
                                image = image,
                                imageRepository = imageRepository,
                                isSelectionMode = selectionMode,
                                isSelected = selectedImages.contains(image),
                                onClick = {
                                    if (selectionMode) {
                                        // En mode sélection, ajouter/retirer de la sélection
                                        selectedImages = if (selectedImages.contains(image)) {
                                            selectedImages - image
                                        } else {
                                            selectedImages + image
                                        }
                                    } else {
                                        // Mode normal, afficher l'image
                                        selectedImage = image
                                        // Charger l'image complète
                                        coroutineScope.launch {
                                            isLoading = true
                                            fullImageData = withContext(Dispatchers.IO) {
                                                imageRepository.getImageData(image.idImage)
                                            }
                                            isLoading = false
                                            if (fullImageData != null) {
                                                showImageDetail = true
                                            }
                                        }
                                    }
                                },
                                onRename = {
                                    selectedImage = image
                                    newImageName = image.imageName
                                    showRenameDialog = true
                                },
                                onDelete = {
                                    selectedImage = image
                                    showDeleteConfirmDialog = true
                                },
                                onMoveToAlbum = {
                                    selectedImage = image
                                    showMoveToAlbumForImageDialog = true  // Utilisez la nouvelle variable
                                }
                            )
                        }
                    }
                }
            }
            
            
            // Dialogue d'ajout d'image
            if (showAddImageDialog) {
                Dialog(onDismissRequest = { showAddImageDialog = false }) {
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
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Ajouter une image",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Button(
                                onClick = {
                                    showAddImageDialog = false
                                    coroutineScope.launch {
                                        val selectedFile = withContext(Dispatchers.IO) {
                                            selectImageFile()
                                        }
                                        
                                        if (selectedFile != null) {
                                            isLoading = true
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val imageBytes = selectedFile.readBytes()
                                                    val mimeType = getMimeType(selectedFile.name)
                                                    
                                                    imageRepository.saveImage(
                                                        imageData = imageBytes,
                                                        imageName = selectedFile.name,
                                                        mimeType = mimeType,
                                                        userId = currentUser.idUser,
                                                        albumId = currentAlbum?.idAlbum
                                                    )
                                                    
                                                    // Recharger les images
                                                    loadImages()
                                                } catch (e: Exception) {
                                                    println("Erreur lors de l'ajout de l'image: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            }
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Choisir une image",
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            TextButton(
                                onClick = { showAddImageDialog = false },
                                modifier = Modifier.fillMaxWidth()
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
            
            // Dialogue de détail d'image
            if (showImageDetail && selectedImage != null) {
                Dialog(
                    onDismissRequest = { 
                        showImageDetail = false
                        fullImageData = null 
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false,
                        usePlatformDefaultWidth = false // Permet d'avoir un dialogue en plein écran
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2A3A))
                    ) {
                        EnhancedImageViewer(
                            currentImage = selectedImage!!,
                            allImages = vaultImages,
                            imageRepository = imageRepository,
                            onClose = {
                                showImageDetail = false
                                fullImageData = null
                            },
                            onImageChange = { newImage ->
                                selectedImage = newImage
                                // La récupération des données se fait dans EnhancedImageViewer
                            }
                        )
                    }
                }
            }
            // Dialogue pour déplacer une image vers un album
            if (showMoveToAlbumForImageDialog && selectedImage != null) {
                val albumRepository = remember { AlbumRepository() }
                var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
                
                // Charger les albums
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        albums = albumRepository.getUserAlbums(currentUser.idUser)
                    }
                }
                
                Dialog(onDismissRequest = { showMoveToAlbumForImageDialog = false }) {
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
                                    // Option pour retirer de l'album
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                coroutineScope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        imageRepository.removeImageFromAlbum(selectedImage!!.idImage)
                                                    }
                                                    loadImages()
                                                    showMoveToAlbumForImageDialog = false
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF757575))
                                        )
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Text(
                                            text = "Aucun album (retirer de l'album)",
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )
                                    }
                                    
                                    Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                                    
                                    // Liste des albums
                                    for (album in albums) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            imageRepository.assignImageToAlbum(selectedImage!!.idImage, album.idAlbum)
                                                        }
                                                        loadImages()
                                                        showMoveToAlbumForImageDialog = false
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(parseColor(album.color.colorHexa)))
                                            )
                                            
                                            Spacer(modifier = Modifier.width(16.dp))
                                            
                                            Text(
                                                text = album.albumName,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
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
                                    onClick = { showMoveToAlbumForImageDialog = false }
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
            
            // Dialogue de renommage
            if (showRenameDialog && selectedImage != null) {
                Dialog(onDismissRequest = { showRenameDialog = false }) {
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
                                text = "Renommer l'image",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            OutlinedTextField(
                                value = newImageName,
                                onValueChange = { newImageName = it },
                                label = { Text("Nom de l'image") },
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
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showRenameDialog = false }
                                ) {
                                    Text(
                                        text = "Annuler",
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (newImageName.isNotBlank()) {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    imageRepository.renameImage(
                                                        selectedImage!!.idImage,
                                                        newImageName
                                                    )
                                                }
                                                loadImages()
                                            }
                                            showRenameDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                                ) {
                                    Text(
                                        text = "Renommer",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Dialogue de confirmation de suppression
            if (showDeleteConfirmDialog && selectedImage != null) {
                Dialog(onDismissRequest = { showDeleteConfirmDialog = false }) {
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
                            // Titre et icône d'avertissement
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirmer la suppression",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Message plus détaillé
                            Text(
                                text = "Vous êtes sur le point de supprimer définitivement l'image :",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Nom de l'image mis en évidence
                            Text(
                                text = selectedImage!!.imageName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A1A2E), RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Avertissement en rouge
                            Text(
                                text = "Cette action est irréversible et l'image sera perdue définitivement.",
                                color = Color(0xFFF44336),
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            // Boutons d'action avec texte plus explicite
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                // Bouton Annuler plus visible
                                OutlinedButton(
                                    onClick = { showDeleteConfirmDialog = false },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color.Transparent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "Annuler",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Bouton Supprimer avec texte plus explicite
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                imageRepository.deleteImage(selectedImage!!.idImage)
                                            }
                                            loadImages()
                                        }
                                        showDeleteConfirmDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Supprimer définitivement",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
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
 * Composant pour afficher une miniature d'image dans la grille
 */
@Composable
fun ImageCard(
    image: Image,
    imageRepository: ImageRepository,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveToAlbum: () -> Unit
) {
    // État pour la miniature bitmap
    var thumbnailData by remember { mutableStateOf<ByteArray?>(null) }
    // État pour contrôler l'affichage du menu
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Charger la miniature
    LaunchedEffect(image) {
        coroutineScope.launch {
            thumbnailData = withContext(Dispatchers.IO) {
                imageRepository.getImageThumbnail(image.idImage)
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
        backgroundColor = Color(0xFF2A2A3A)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Afficher la miniature ou un placeholder
            if (thumbnailData != null) {
                // Utiliser un composant personnalisé pour éviter les problèmes d'asImageBitmap
                ByteArrayImage(
                    imageData = thumbnailData!!,
                    contentDescription = image.imageName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🖼️",
                        fontSize = 32.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Indicateur d'album (uniquement si l'image est dans un album)
            if (image.album != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(parseColor(image.album!!.color.colorHexa)))
                    )
                }
            }
            
            // Indicateur de sélection (uniquement en mode sélection)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xAA000000), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.Add,
                            contentDescription = if (isSelected) "Sélectionné" else "Non sélectionné",
                            tint = if (isSelected) MaterialTheme.colors.primary else Color.White
                        )
                    }
                }
            }
            
            // Overlay avec le nom de l'image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xAA000000))
                    .padding(8.dp)
            ) {
                Text(
                    text = image.imageName,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
            
            // Bouton avec trois points (menu)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xAA000000), CircleShape)
                ) {
                    // Icône de trois points verticaux (menu)
                    Column(
                        modifier = Modifier.size(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
                
                // Menu dropdown
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF2A2A3A))
                        .width(180.dp)
                ) {
                    // Option Déplacer vers un album
                    DropdownMenuItem(
                        onClick = {
                            onMoveToAlbum()
                            showMenu = false
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Déplacer vers un album",
                                color = Color.White
                            )
                        }
                    }
                    
                    // Option Renommer
                    DropdownMenuItem(
                        onClick = {
                            onRename()
                            showMenu = false
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    }
                    
                    // Option Supprimer
                    DropdownMenuItem(
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Supprimer",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composant personnalisé pour afficher une image à partir d'un ByteArray
 * sans utiliser asImageBitmap qui pose problème
 */
@Composable
fun ByteArrayImage(
    imageData: ByteArray,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Charger l'image dans un effet secondaire
    LaunchedEffect(imageData) {
        withContext(Dispatchers.IO) {
            try {
                // Utiliser la méthode de ImageRepository si vous l'avez déjà
                // Sinon, convertir directement ici
                val imageStream = ByteArrayInputStream(imageData)
                val bufferedImage = ImageIO.read(imageStream)
                if (bufferedImage != null) {
                    bitmap = bufferedImage.toComposeImageBitmap()
                }
            } catch (e: Exception) {
                println("Erreur lors de la conversion de l'image: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    Box(
        modifier = modifier.background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            // Afficher l'image
            Image(
                bitmap = bitmap!!,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            // Afficher un indicateur de chargement ou un placeholder
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
fun EnhancedImageViewer(
    currentImage: Image,
    allImages: List<Image>,
    imageRepository: ImageRepository,
    onClose: () -> Unit,
    onImageChange: (Image) -> Unit
) {
    // État pour les données de l'image
    var fullImageData by remember { mutableStateOf<ByteArray?>(null) }
    
    // État pour le mode plein écran
    var isFullScreen by remember { mutableStateOf(false) }
    
    // Obtenir l'index de l'image actuelle et calculer les indices précédent/suivant
    val currentIndex = allImages.indexOfFirst { it.idImage == currentImage.idImage }
    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex < allImages.size - 1
    
    // Charger l'image
    LaunchedEffect(currentImage) {
        fullImageData = null // Réinitialiser pendant le chargement
        fullImageData = withContext(Dispatchers.IO) {
            imageRepository.getImageData(currentImage.idImage)
        }
    }
    
    // Fonction pour naviguer vers l'image précédente
    fun navigateToPrevious() {
        if (hasPrevious) {
            onImageChange(allImages[currentIndex - 1])
        }
    }
    
    // Fonction pour naviguer vers l'image suivante
    fun navigateToNext() {
        if (hasNext) {
            onImageChange(allImages[currentIndex + 1])
        }
    }
    
    // Intercepter les événements clavier
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionLeft -> {
                        navigateToPrevious()
                        true
                    }
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionRight -> {
                        navigateToNext()
                        true
                    }
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape -> {
                        if (isFullScreen) {
                            isFullScreen = false
                        } else {
                            onClose()
                        }
                        true
                    }
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F || 
                    keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter -> {
                        isFullScreen = !isFullScreen
                        true
                    }
                    else -> false
                }
            }
            .focusRequester(remember { FocusRequester() })
            .focusable()
    ) {
        // Fond
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1E))
        ) {
            // Contenu principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreen) 0.dp else 16.dp)
            ) {
                // Barre de titre (visible uniquement si pas en plein écran)
                if (!isFullScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentImage.imageName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Boutons d'action
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Bouton fermer
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF3A3A3A), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                
                // Conteneur de l'image avec flèches de navigation
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Image
                    if (fullImageData != null) {
                        ByteArrayImage(
                            imageData = fullImageData!!,
                            contentDescription = currentImage.imageName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colors.primary)
                        }
                    }
                    
                    // Superposition semi-transparente pour contrôles en mode plein écran
                    if (isFullScreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isFullScreen = false
                                    }
                                }
                        ) {
                            // Bouton quitter plein écran (en haut à droite)
                            IconButton(
                                onClick = { isFullScreen = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(44.dp)
                                    .background(Color(0x99000000), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Quitter le mode plein écran",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Flèches de navigation (toujours visibles)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flèche précédente
                        if (hasPrevious) {
                            IconButton(
                                onClick = { navigateToPrevious() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0x99000000), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Image précédente",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        
                        // Espace central
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Flèche suivante
                        if (hasNext) {
                            IconButton(
                                onClick = { navigateToNext() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0x99000000), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Image suivante",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
                
                // Barre inférieure (visible uniquement si pas en plein écran)
                if (!isFullScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Indicateur de position
                        Text(
                            text = "Image ${currentIndex + 1} sur ${allImages.size}",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        // Bouton plein écran supplémentaire
                        Button(
                            onClick = { isFullScreen = true },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3A3A3A))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Plein écran",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}