package com.example.secretvault.ui.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.secretvault.model.SecretNote
import com.example.secretvault.model.VaultMode
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultPurple
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultEmptyState
import com.example.secretvault.ui.common.VaultSearchField
import com.example.secretvault.ui.common.VaultTopAppBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretNotesScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY

    var notesList by remember { mutableStateOf<List<SecretNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Editor State
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<SecretNote?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("General") }
    var editIsPinned by remember { mutableStateOf(false) }

    fun refreshNotes() {
        scope.launch {
            isLoading = true
            notesList = repository.getNotes(isDecoy)
            isLoading = false
        }
    }

    LaunchedEffect(vaultMode) {
        refreshNotes()
    }

    val categories = listOf("All", "General", "Passwords", "Banking", "Private", "Ideas")

    val filteredNotes = notesList.filter { note ->
        val matchesCategory = selectedCategory == "All" || note.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    fun addSampleNote() {
        val key = securityManager.activeSecretKey ?: return
        scope.launch {
            isLoading = true
            val sample = SecretNote(
                id = UUID.randomUUID().toString(),
                title = "Secret Recovery Keys",
                content = "1. Master Backup: 8841-9920-1148\n2. Crypto Wallet Seed: orange apple river galaxy\n3. Storage Safe Code: 9942",
                category = "Passwords",
                dateCreatedMs = System.currentTimeMillis(),
                dateModifiedMs = System.currentTimeMillis(),
                isPinned = true,
                isDecoy = isDecoy
            )
            repository.saveNote(sample, isDecoy)
            refreshNotes()
            Toast.makeText(context, "Encrypted sample note created!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            VaultTopAppBar(
                title = "Secret Notes & Passwords",
                subtitle = "${notesList.size} encrypted notes",
                accentColor = VaultPurple,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { addSampleNote() }) {
                        Text("+ Sample", color = VaultPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNote = null
                    editTitle = ""
                    editContent = ""
                    editCategory = "General"
                    editIsPinned = false
                    showEditorDialog = true
                },
                containerColor = VaultPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            VaultSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search notes, logins, secrets...",
                accentColor = VaultPurple,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VaultPurple.copy(alpha = 0.2f),
                            selectedLabelColor = VaultPurple,
                            containerColor = VaultCardBackground,
                            labelColor = VaultTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Notes List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VaultPurple
                    )
                } else if (filteredNotes.isEmpty()) {
                    VaultEmptyState(
                        icon = Icons.Default.NoteAlt,
                        title = "No Encrypted Notes",
                        description = "Save passwords, crypto recovery seeds, personal diary entries, or private checklists securely.",
                        actionButtonText = "Create Note",
                        onActionClick = {
                            editingNote = null
                            editTitle = ""
                            editContent = ""
                            editCategory = "General"
                            editIsPinned = false
                            showEditorDialog = true
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                onClick = {
                                    editingNote = note
                                    editTitle = note.title
                                    editContent = note.content
                                    editCategory = note.category
                                    editIsPinned = note.isPinned
                                    showEditorDialog = true
                                },
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(note.title, note.content))
                                    Toast.makeText(context, "Copied '${note.title}' to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    scope.launch {
                                        repository.deleteNote(note.id, isDecoy)
                                        refreshNotes()
                                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
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
    }

    // Editor Dialog
    if (showEditorDialog) {
        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            containerColor = VaultSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingNote == null) "New Secret Note" else "Edit Note",
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { editIsPinned = !editIsPinned }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin",
                            tint = if (editIsPinned) VaultPurple else VaultTextMuted
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title / Account") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultPurple,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category selection row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(listOf("General", "Passwords", "Banking", "Private", "Ideas")) { cat ->
                            FilterChip(
                                selected = editCategory == cat,
                                onClick = { editCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultPurple.copy(alpha = 0.25f),
                                    selectedLabelColor = VaultPurple,
                                    containerColor = VaultCardBackground,
                                    labelColor = VaultTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Secret Content (Passwords, PINs, Keys)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultPurple,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.trim().isNotEmpty() || editContent.trim().isNotEmpty()) {
                            val note = SecretNote(
                                id = editingNote?.id ?: UUID.randomUUID().toString(),
                                title = editTitle.ifBlank { "Untitled Note" },
                                content = editContent,
                                category = editCategory,
                                isPinned = editIsPinned,
                                isDecoy = isDecoy
                            )
                            scope.launch {
                                repository.saveNote(note, isDecoy)
                                refreshNotes()
                                showEditorDialog = false
                                Toast.makeText(context, "Note encrypted & saved", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Note cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultPurple)
                ) {
                    Text("Save Secret", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditorDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: SecretNote,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = VaultPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = note.title,
                        color = VaultTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(VaultPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = note.category,
                        color = VaultPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content,
                color = VaultTextSecondary,
                fontSize = 13.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(note.dateModifiedMs))
                Text(
                    text = dateStr,
                    color = VaultTextMuted,
                    fontSize = 11.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = VaultTextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultRed.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
