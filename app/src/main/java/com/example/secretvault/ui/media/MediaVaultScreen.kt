package com.example.secretvault.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultMode
import com.example.secretvault.model.VaultType
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultEmptyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaVaultScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY
    val key = securityManager.activeSecretKey

    var mediaItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItemForViewer by remember { mutableStateOf<VaultItem?>(null) }
    val selectedItemIds = remember { mutableStateMapOf<String, Boolean>() }
    val isMultiSelectMode = selectedItemIds.any { it.value }

    fun refreshList() {
        scope.launch {
            isLoading = true
            val all = repository.getItems(isDecoy)
            mediaItems = all.filter { !it.isInTrash && (it.vaultType == VaultType.PHOTO || it.vaultType == VaultType.VIDEO) }
            isLoading = false
        }
    }

    LaunchedEffect(vaultMode) {
        refreshList()
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty() || key == null) return
        scope.launch {
            isLoading = true
            var count = 0
            for (uri in uris) {
                try {
                    val name = getFileNameFromUri(context, uri) ?: "imported_media_${System.currentTimeMillis()}.jpg"
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val type = if (mime.startsWith("video")) VaultType.VIDEO else VaultType.PHOTO
                    repository.importFile(uri, name, mime, type, key, isDecoy)
                    count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isLoading = false
            refreshList()
            Toast.makeText(context, "Encrypted and imported $count items!", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        importUris(uris)
    }

    val fallbackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        importUris(uris)
    }

    fun openMediaPicker() {
        try {
            photoPickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                )
            )
        } catch (e: Exception) {
            fallbackPickerLauncher.launch("*/*")
        }
    }

    fun addSamplePhoto() {
        if (key == null) return
        scope.launch {
            isLoading = true
            try {
                val bytes = createSamplePhotoBytes()
                repository.importRawBytes(
                    bytes = bytes,
                    name = "Secret_Photo_${System.currentTimeMillis() % 10000}.jpg",
                    mimeType = "image/jpeg",
                    type = VaultType.PHOTO,
                    key = key,
                    isDecoy = isDecoy
                )
                refreshList()
                Toast.makeText(context, "Encrypted sample photo added!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destUri ->
        if (destUri != null && key != null && selectedItemForViewer != null) {
            scope.launch {
                val success = repository.exportFile(selectedItemForViewer!!, destUri, key)
                if (success) {
                    Toast.makeText(context, "File exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (selectedItemForViewer != null && key != null) {
        // Fullscreen Viewer
        FullscreenImageViewer(
            item = selectedItemForViewer!!,
            key = key,
            repository = repository,
            onClose = { selectedItemForViewer = null },
            onExport = {
                exportLauncher.launch(selectedItemForViewer!!.name)
            },
            onDelete = {
                scope.launch {
                    repository.moveToTrash(selectedItemForViewer!!)
                    selectedItemForViewer = null
                    refreshList()
                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isMultiSelectMode) "${selectedItemIds.count { it.value }} Selected" else "Photos & Videos",
                                color = VaultTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isMultiSelectMode) {
                                Text(
                                    text = "${mediaItems.size} encrypted items",
                                    color = VaultEmerald,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isMultiSelectMode) {
                                selectedItemIds.clear()
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = VaultTextPrimary
                            )
                        }
                    },
                    actions = {
                        if (isMultiSelectMode) {
                            IconButton(onClick = {
                                scope.launch {
                                    val toDelete = mediaItems.filter { selectedItemIds[it.id] == true }
                                    for (item in toDelete) {
                                        repository.moveToTrash(item)
                                    }
                                    selectedItemIds.clear()
                                    refreshList()
                                    Toast.makeText(context, "Moved ${toDelete.size} items to Trash", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = VaultRed)
                            }
                        } else {
                            TextButton(onClick = { addSamplePhoto() }) {
                                Text("+ Sample", color = VaultEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBackground)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { openMediaPicker() },
                    containerColor = VaultEmerald,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Photos or Videos", modifier = Modifier.size(28.dp))
                }
            },
            containerColor = VaultBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VaultEmerald
                    )
                } else if (mediaItems.isEmpty()) {
                    VaultEmptyState(
                        icon = Icons.Default.PhotoLibrary,
                        title = "No Hidden Photos or Videos",
                        description = "Tap '+' to encrypt & hide your private photos or videos with AES-256 military security.",
                        actionButtonText = "Import Photos / Videos",
                        onActionClick = { openMediaPicker() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(mediaItems, key = { it.id }) { item ->
                            val isSelected = selectedItemIds[item.id] == true
                            MediaThumbnailItem(
                                item = item,
                                key = key,
                                repository = repository,
                                isSelected = isSelected,
                                isMultiSelect = isMultiSelectMode,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        selectedItemIds[item.id] = !isSelected
                                    } else {
                                        selectedItemForViewer = item
                                    }
                                },
                                onLongClick = {
                                    selectedItemIds[item.id] = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaThumbnailItem(
    item: VaultItem,
    key: SecretKey?,
    repository: VaultRepository,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.id, key) {
        if (key != null && bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                repository.loadDecryptedBitmap(item, key)
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(VaultCardBackground)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, VaultEmerald, RoundedCornerShape(8.dp)) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = if (item.vaultType == VaultType.VIDEO) Icons.Default.Movie else Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = VaultTextMuted,
                modifier = Modifier.size(32.dp)
            )
        }

        if (item.vaultType == VaultType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (isMultiSelect) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) VaultEmerald else Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenImageViewer(
    item: VaultItem,
    key: SecretKey,
    repository: VaultRepository,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var fullBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(item.id) {
        fullBitmap = withContext(Dispatchers.IO) {
            repository.loadDecryptedBitmap(item, key)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.name,
                        color = VaultTextPrimary,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export to Gallery", tint = VaultEmerald)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete to Trash", tint = VaultRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.8f))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (fullBitmap != null) {
                Image(
                    bitmap = fullBitmap!!.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                CircularProgressIndicator(color = VaultEmerald)
            }
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path?.let {
            val cut = it.lastIndexOf('/')
            if (cut != -1) it.substring(cut + 1) else it
        }
    }
    return result
}

fun createSamplePhotoBytes(): ByteArray {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, 400f, 400f, paint)
    paint.color = android.graphics.Color.rgb(16, 185, 129) // VaultEmerald
    canvas.drawCircle(200f, 160f, 80f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 24f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("TOP SECRET PHOTO", 200f, 300f, paint)
    paint.textSize = 16f
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawText("AES-256 Encrypted", 200f, 335f, paint)

    val bos = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
    return bos.toByteArray()
}
