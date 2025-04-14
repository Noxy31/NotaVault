package org.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.*
import org.example.entities.Image
import org.example.entities.User
import org.jetbrains.skia.Image as SkiaImage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
    
    // États pour les dialogues
    var showImageDetail by remember { mutableStateOf(false) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // État pour le chargement
    var isLoading by remember { mutableStateOf(false) }
    
    // Nouveau nom pour le renommage
    var newImageName by remember { mutableStateOf("") }
    
    // Fonction pour charger les images
    fun loadImages() {
        isLoading = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                vaultImages = imageRepository.getUserImages(currentUser.idUser)
            }
            isLoading = false
        }
    }
    
    // Charger les images au démarrage
    LaunchedEffect(Unit) {
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
                // Barre d'en-tête
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
                        text = "Coffre d'images",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Bouton ajouter
                    IconButton(
                        onClick = { showAddImageDialog = true },
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
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucune image dans le coffre",
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
                            ImageThumbnail(
                                image = image,
                                imageRepository = imageRepository,
                                onClick = {
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
                                },
                                onRename = {
                                    selectedImage = image
                                    newImageName = image.imageName
                                    showRenameDialog = true
                                },
                                onDelete = {
                                    selectedImage = image
                                    showDeleteConfirmDialog = true
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
                                                        userId = currentUser.idUser
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
                                    imageVector = Icons.Default.FolderOpen,
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
            if (showImageDetail && selectedImage != null && fullImageData != null) {
                Dialog(onDismissRequest = { 
                    showImageDetail = false
                    fullImageData = null 
                }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.8f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2A2A3A))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // En-tête avec le nom et les actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedImage!!.imageName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = { showImageDetail = false },
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
                            
                            // Affichage de l'image
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A2E))
                            ) {
                                // Convertir les données binaires en ImageBitmap
                                val imageBitmap = remember(fullImageData) {
                                    fullImageData?.let {
                                        SkiaImage.makeFromEncoded(it).asImageBitmap()
                                    }
                                }
                                
                                if (imageBitmap != null) {
                                    Image(
                                        bitmap = imageBitmap,
                                        contentDescription = selectedImage!!.imageName,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colors.primary)
                                    }
                                }
                            }
                            
                            // Boutons d'action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        newImageName = selectedImage!!.imageName
                                        showImageDetail = false
                                        showRenameDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3A3A3A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renommer",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Renommer",
                                        color = Color.White
                                    )
                                }
                                
                                Button(
                                    onClick = {
                                        showImageDetail = false
                                        showDeleteConfirmDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
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
                            Text(
                                text = "Supprimer l'image",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "Êtes-vous sûr de vouloir supprimer cette image ? Cette action est irréversible.",
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showDeleteConfirmDialog = false }
                                ) {
                                    Text(
                                        text = "Annuler",
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
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
    }
}

/**
 * Composant pour afficher une miniature d'image dans la grille
 */
@Composable
fun ImageThumbnail(
    image: Image,
    imageRepository: ImageRepository,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    // État pour la miniature bitmap
    var thumbnailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Charger la miniature
    LaunchedEffect(image) {
        coroutineScope.launch {
            val thumbnailData = withContext(Dispatchers.IO) {
                imageRepository.getImageThumbnail(image.idImage)
            }
            
            if (thumbnailData != null) {
                thumbnailBitmap = SkiaImage.makeFromEncoded(thumbnailData).asImageBitmap()
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
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!,
                    contentDescription = image.imageName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Overlay avec le nom de l'image et les actions
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
            
            // Actions rapides
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xAA000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Renommer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xAAC62828), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}