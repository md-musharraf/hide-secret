package com.example.secretvault.ui.document

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultMode
import com.example.secretvault.model.VaultType
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultAmber
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBlue
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultCyan
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultPurple
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultEmptyState
import com.example.secretvault.ui.common.VaultTopAppBar
import com.example.secretvault.ui.media.getFileNameFromUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY
    val key = securityManager.activeSecretKey

    var docItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var previewDocItem by remember { mutableStateOf<VaultItem?>(null) }
    var previewDocContent by remember { mutableStateOf<String?>(null) }
    var itemToExport by remember { mutableStateOf<VaultItem?>(null) }

    fun refreshList() {
        scope.launch {
            isLoading = true
            val all = repository.getItems(isDecoy)
            docItems = all.filter { !it.isInTrash && it.vaultType == VaultType.DOCUMENT }
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
                    val name = getFileNameFromUri(context, uri) ?: "doc_${System.currentTimeMillis()}.pdf"
                    val mime = context.contentResolver.getType(uri) ?: "*/*"
                    repository.importFile(uri, name, mime, VaultType.DOCUMENT, key, isDecoy)
                    count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isLoading = false
            refreshList()
            Toast.makeText(context, "Encrypted & imported $count documents", Toast.LENGTH_SHORT).show()
        }
    }

    val docOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        importUris(uris)
    }

    val docGetContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        importUris(uris)
    }

    fun openDocPicker() {
        try {
            docOpenLauncher.launch(arrayOf("*/*"))
        } catch (e: Exception) {
            docGetContentLauncher.launch("*/*")
        }
    }

    fun addSampleDocument() {
        if (key == null) return
        scope.launch {
            isLoading = true
            try {
                val sampleDocText = """
                    CONFIDENTIAL VAULT DOCUMENT
                    ----------------------------------------
                    Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                    Classification: TOP SECRET // AES-256-GCM
                    
                    Emergency Backup Keys:
                    1. 9284-1182-4401
                    2. 7719-2041-9938
                    3. 6620-8451-3310
                    
                    Stored inside Secret Vault with military encryption.
                """.trimIndent()

                repository.importRawBytes(
                    bytes = sampleDocText.toByteArray(Charsets.UTF_8),
                    name = "Secret_Credentials_${System.currentTimeMillis() % 10000}.txt",
                    mimeType = "text/plain",
                    type = VaultType.DOCUMENT,
                    key = key,
                    isDecoy = isDecoy
                )
                refreshList()
                Toast.makeText(context, "Encrypted sample document created!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destUri ->
        if (destUri != null && key != null && itemToExport != null) {
            scope.launch {
                val success = repository.exportFile(itemToExport!!, destUri, key)
                if (success) {
                    Toast.makeText(context, "Document exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
                itemToExport = null
            }
        }
    }

    Scaffold(
        topBar = {
            VaultTopAppBar(
                title = "Document Safe",
                subtitle = "${docItems.size} encrypted files",
                accentColor = VaultBlue,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { addSampleDocument() }) {
                        Text("+ Sample", color = VaultBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openDocPicker() },
                containerColor = VaultBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Document", modifier = Modifier.size(28.dp))
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
                    color = VaultBlue
                )
            } else if (docItems.isEmpty()) {
                VaultEmptyState(
                    icon = Icons.Default.Description,
                    title = "No Hidden Documents",
                    description = "Hide confidential PDFs, invoices, contracts, spreadsheets, or text files securely.",
                    actionButtonText = "Import Document",
                    onActionClick = { openDocPicker() },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(docItems, key = { it.id }) { item ->
                        DocumentItemRow(
                            item = item,
                            onClick = {
                                if (key != null) {
                                    val ext = item.name.substringAfterLast('.', "").lowercase()
                                    if (ext in listOf("txt", "md", "json", "csv", "log", "xml", "html")) {
                                        scope.launch {
                                            previewDocContent = repository.loadDecryptedText(item, key)
                                            previewDocItem = item
                                        }
                                    } else {
                                        // For PDF/Word, offer export/open
                                        itemToExport = item
                                        exportLauncher.launch(item.name)
                                    }
                                }
                            },
                            onExport = {
                                itemToExport = item
                                exportLauncher.launch(item.name)
                            },
                            onDelete = {
                                scope.launch {
                                    repository.moveToTrash(item)
                                    refreshList()
                                    Toast.makeText(context, "Document moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }

    // In-app Text Document Preview Dialog
    if (previewDocItem != null && previewDocContent != null) {
        AlertDialog(
            onDismissRequest = {
                previewDocItem = null
                previewDocContent = null
            },
            containerColor = VaultSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = previewDocItem!!.name,
                        color = VaultTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        previewDocItem = null
                        previewDocContent = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextMuted)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VaultCardBackground)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = previewDocContent!!,
                        color = VaultTextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToExport = previewDocItem
                        exportLauncher.launch(previewDocItem!!.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultBlue)
                ) {
                    Text("Export Document", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    previewDocItem = null
                    previewDocContent = null
                }) {
                    Text("Close", color = VaultTextSecondary)
                }
            }
        )
    }
}

@Composable
fun DocumentItemRow(
    item: VaultItem,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = item.name.substringAfterLast('.', "").uppercase().ifEmpty { "DOC" }
    val badgeColor = when (ext) {
        "PDF" -> VaultRed
        "TXT", "MD" -> VaultBlue
        "XLS", "XLSX", "CSV" -> VaultEmerald
        "DOC", "DOCX" -> VaultPurple
        "ZIP", "RAR", "7Z" -> VaultAmber
        else -> VaultCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ext.take(4),
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.name,
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateAddedMs))
                    val sizeKb = item.fileSizeBytes / 1024
                    Text(
                        text = "$dateStr • ${sizeKb}KB",
                        color = VaultTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExport) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = VaultBlue.copy(alpha = 0.8f))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultRed.copy(alpha = 0.7f))
                }
            }
        }
    }
}
