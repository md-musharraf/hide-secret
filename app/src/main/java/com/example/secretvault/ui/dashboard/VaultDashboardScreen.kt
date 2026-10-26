package com.example.secretvault.ui.dashboard

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultMode
import com.example.secretvault.model.VaultStats
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
import com.example.secretvault.ui.common.QuickGuideDialog
import com.example.secretvault.ui.common.StorageBreakdownBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    repository: VaultRepository,
    onNavigateToMedia: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onLockVault: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY
    var stats by remember { mutableStateOf(VaultStats()) }
    var showGuideDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vaultMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    stats = repository.getStats(isDecoy)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(vaultMode) {
        stats = repository.getStats(isDecoy)
    }

    if (showGuideDialog) {
        QuickGuideDialog(onDismiss = { showGuideDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDecoy) VaultAmber.copy(alpha = 0.2f) else VaultEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDecoy) Icons.Default.Shield else Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = if (isDecoy) VaultAmber else VaultEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isDecoy) "Personal Space" else "Secret Vault",
                                color = VaultTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDecoy) "Standard Mode" else "AES-256 Armed • Zero Leak",
                                color = if (isDecoy) VaultAmber else VaultEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VaultCyan.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Quick Guide",
                            tint = VaultCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VaultRed.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Panic Lock",
                            tint = VaultRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBackground)
            )
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stats Banner across both columns
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        VaultEmerald.copy(alpha = 0.15f),
                                        VaultCyan.copy(alpha = 0.05f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PROTECTED ASSETS",
                                    color = VaultEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val totalItems = stats.photosCount + stats.videosCount + stats.audiosCount + stats.docsCount + stats.notesCount
                                Text(
                                    text = "$totalItems Hidden Secrets",
                                    color = VaultTextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Encrypted in military sandbox",
                                    color = VaultTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(VaultEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = VaultEmerald,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        StorageBreakdownBar(stats = stats, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }

            // Category 1: Photos & Videos
            item {
                VaultCategoryCard(
                    title = "Photos & Videos",
                    subtitle = "${stats.photosCount + stats.videosCount} files",
                    icon = Icons.Default.PhotoLibrary,
                    color = VaultEmerald,
                    onClick = onNavigateToMedia
                )
            }

            // Category 2: Audios & Voice
            item {
                VaultCategoryCard(
                    title = "Secret Audio",
                    subtitle = "${stats.audiosCount} recordings",
                    icon = Icons.Default.Audiotrack,
                    color = VaultCyan,
                    onClick = onNavigateToAudio
                )
            }

            // Category 3: Documents
            item {
                VaultCategoryCard(
                    title = "Document Safe",
                    subtitle = "${stats.docsCount} files",
                    icon = Icons.Default.Description,
                    color = VaultBlue,
                    onClick = onNavigateToDocuments
                )
            }

            // Category 4: Notes & Passwords
            item {
                VaultCategoryCard(
                    title = "Secret Notes",
                    subtitle = "${stats.notesCount} encrypted",
                    icon = Icons.Default.NoteAlt,
                    color = VaultPurple,
                    onClick = onNavigateToNotes
                )
            }

            // Category 5: App Locker
            item {
                VaultCategoryCard(
                    title = "App Locker",
                    subtitle = "Disguise & Protect",
                    icon = Icons.Default.Apps,
                    color = VaultAmber,
                    onClick = onNavigateToAppLock
                )
            }

            // Category 6: Security & Break-in
            item {
                VaultCategoryCard(
                    title = "Security Hub",
                    subtitle = "PIN, Decoy & Alerts",
                    icon = Icons.Default.Security,
                    color = VaultRed,
                    onClick = onNavigateToSettings
                )
            }

            // Recycle Bin Card across both columns
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToTrash() },
                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VaultTextMuted.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = VaultTextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Recycle Bin",
                                    color = VaultTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${stats.trashCount} trashed items (recover or shred)",
                                    color = VaultTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VaultCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = VaultTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
