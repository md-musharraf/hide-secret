package com.example.secretvault.ui.trash

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultMode
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultConfirmDialog
import com.example.secretvault.ui.common.VaultEmptyState
import com.example.secretvault.ui.common.VaultTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    vaultMode: VaultMode,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY

    var trashedItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }

    fun refreshTrash() {
        scope.launch {
            isLoading = true
            val all = repository.getItems(isDecoy)
            trashedItems = all.filter { it.isInTrash }
            isLoading = false
        }
    }

    LaunchedEffect(vaultMode) {
        refreshTrash()
    }

    Scaffold(
        topBar = {
            VaultTopAppBar(
                title = "Recycle Bin",
                subtitle = "${trashedItems.size} items in trash",
                accentColor = VaultRed,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (trashedItems.isNotEmpty()) {
                        IconButton(onClick = { showEmptyBinDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty Bin", tint = VaultRed)
                        }
                    }
                }
            )
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = VaultEmerald)
            } else if (trashedItems.isEmpty()) {
                VaultEmptyState(
                    icon = Icons.Default.DeleteSweep,
                    title = "Recycle Bin is Empty",
                    description = "Items moved to trash will appear here. You can restore them or securely shred them with zeroes.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trashedItems, key = { it.id }) { item ->
                        TrashedItemRow(
                            item = item,
                            onRestore = {
                                scope.launch {
                                    repository.restoreFromTrash(item)
                                    refreshTrash()
                                    Toast.makeText(context, "Restored '${item.name}'", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onPermanentDelete = {
                                scope.launch {
                                    repository.permanentlyDelete(item)
                                    refreshTrash()
                                    Toast.makeText(context, "Shredded permanently", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEmptyBinDialog) {
        VaultConfirmDialog(
            title = "Empty Recycle Bin?",
            message = "All ${trashedItems.size} items will be permanently shredded with zeroes. This action cannot be undone.",
            confirmText = "Shred All",
            isDestructive = true,
            onConfirm = {
                scope.launch {
                    for (item in trashedItems) {
                        repository.permanentlyDelete(item)
                    }
                    showEmptyBinDialog = false
                    refreshTrash()
                    Toast.makeText(context, "Recycle bin emptied and shredded", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showEmptyBinDialog = false }
        )
    }
}

@Composable
fun TrashedItemRow(
    item: VaultItem,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = VaultTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateAddedMs))
                Text(
                    text = "${item.vaultType.name} • $dateStr",
                    color = VaultTextMuted,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = VaultEmerald)
                }
                IconButton(onClick = onPermanentDelete) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Permanent Delete", tint = VaultRed)
                }
            }
        }
    }
}
