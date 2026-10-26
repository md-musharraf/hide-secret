package com.example.secretvault.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.model.VaultStats
import com.example.secretvault.theme.VaultAmber
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBlue
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultCyan
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultPurple
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary

/**
 * DRY Standardized Top Navigation Bar for all Vault screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultTopAppBar(
    title: String,
    subtitle: String? = null,
    accentColor: Color = VaultEmerald,
    onNavigateBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    color = VaultTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VaultTextPrimary
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBackground)
    )
}

/**
 * DRY Empty State placeholder with icon, description, and optional action button.
 */
@Composable
fun VaultEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VaultCardBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VaultTextMuted,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            color = VaultTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            color = VaultTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionButtonText, color = VaultEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * DRY Standardized Search input field.
 */
@Composable
fun VaultSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search...",
    accentColor: Color = VaultEmerald,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = VaultTextMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultTextMuted, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = VaultTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = VaultBorder,
            focusedContainerColor = VaultCardBackground,
            unfocusedContainerColor = VaultCardBackground,
            focusedTextColor = VaultTextPrimary,
            unfocusedTextColor = VaultTextPrimary
        )
    )
}

/**
 * DRY Confirmation Alert Dialog.
 */
@Composable
fun VaultConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Text(text = title, color = VaultTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, color = VaultTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) VaultRed else VaultEmerald
                )
            ) {
                Text(confirmText, color = if (isDestructive) Color.White else Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}

/**
 * Visual multi-color storage distribution progress bar.
 */
@Composable
fun StorageBreakdownBar(
    stats: VaultStats,
    modifier: Modifier = Modifier
) {
    val totalCount = (stats.photosCount + stats.videosCount + stats.audiosCount + stats.docsCount + stats.notesCount).coerceAtLeast(1)
    val photosWeight = (stats.photosCount + stats.videosCount).toFloat() / totalCount
    val audiosWeight = stats.audiosCount.toFloat() / totalCount
    val docsWeight = stats.docsCount.toFloat() / totalCount
    val notesWeight = stats.notesCount.toFloat() / totalCount

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VaultCardBackground)
        ) {
            if (photosWeight > 0f) Box(modifier = Modifier.weight(photosWeight).height(8.dp).background(VaultEmerald))
            if (audiosWeight > 0f) Box(modifier = Modifier.weight(audiosWeight).height(8.dp).background(VaultCyan))
            if (docsWeight > 0f) Box(modifier = Modifier.weight(docsWeight).height(8.dp).background(VaultBlue))
            if (notesWeight > 0f) Box(modifier = Modifier.weight(notesWeight).height(8.dp).background(VaultPurple))
            if (totalCount == 1 && stats.photosCount == 0 && stats.audiosCount == 0 && stats.docsCount == 0 && stats.notesCount == 0) {
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(VaultTextMuted.copy(alpha = 0.3f)))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StorageLegendItem(color = VaultEmerald, label = "Photos (${stats.photosCount + stats.videosCount})")
            StorageLegendItem(color = VaultCyan, label = "Audio (${stats.audiosCount})")
            StorageLegendItem(color = VaultBlue, label = "Docs (${stats.docsCount})")
            StorageLegendItem(color = VaultPurple, label = "Notes (${stats.notesCount})")
        }
    }
}

@Composable
private fun StorageLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = VaultTextMuted, fontSize = 10.sp)
    }
}

/**
 * Quick Tips & Onboarding Guide Modal for easy user understanding.
 */
@Composable
fun QuickGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = VaultEmerald, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secret Vault Guide", color = VaultTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GuideItem(
                    icon = Icons.Default.Calculate,
                    color = VaultCyan,
                    title = "Stealth Calculator Disguise",
                    desc = "The app opens as a real calculator. Type your PIN and press '=' to unlock the hidden vault."
                )
                GuideItem(
                    icon = Icons.Default.Shield,
                    color = VaultAmber,
                    title = "Decoy Vault (Plausible Deniability)",
                    desc = "Under coercion, entering your Decoy PIN opens an innocent fake vault with harmless notes and dummy files."
                )
                GuideItem(
                    icon = Icons.Default.EnhancedEncryption,
                    color = VaultEmerald,
                    title = "Military AES-256 Encryption",
                    desc = "All photos, audios, documents, and notes are encrypted with 256-bit AES-GCM. Unencrypted files never touch disk."
                )
                GuideItem(
                    icon = Icons.Default.ScreenRotation,
                    color = VaultRed,
                    title = "Panic Flip & Shake Lock",
                    desc = "Flip your phone facedown on a table or shake it vigorously to instantly lock the vault and return to Calculator."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
            ) {
                Text("Got It!", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GuideItem(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = VaultTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = VaultTextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
