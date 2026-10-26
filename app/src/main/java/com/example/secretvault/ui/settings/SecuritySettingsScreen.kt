package com.example.secretvault.ui.settings

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.model.IntruderLog
import com.example.secretvault.model.VaultMode
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultAmber
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultCyan
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Dialog States
    var showChangeMasterPinDialog by remember { mutableStateOf(false) }
    var newMasterPin by remember { mutableStateOf("") }
    var confirmMasterPin by remember { mutableStateOf("") }

    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var newDecoyPin by remember { mutableStateOf("") }

    var showIntruderLogsDialog by remember { mutableStateOf(false) }
    var intruderLogs by remember { mutableStateOf(securityManager.getIntruderLogs()) }

    var calcDisguise by remember { mutableStateOf(securityManager.isCalculatorDisguise) }
    var flipLock by remember { mutableStateOf(securityManager.isFlipToLockEnabled) }
    var shakeLock by remember { mutableStateOf(securityManager.isShakeToLockEnabled) }
    var screenshotBlock by remember { mutableStateOf(securityManager.isScreenshotBlocked) }
    var biometricUnlock by remember { mutableStateOf(securityManager.isBiometricEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Center",
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBackground)
            )
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader("PIN & VAULT CREDENTIALS")
            }

            // Change Master PIN
            item {
                SettingActionCard(
                    title = "Change Master PIN",
                    subtitle = "Update the 4-8 digit primary vault password",
                    icon = Icons.Default.Key,
                    iconColor = VaultEmerald,
                    onClick = {
                        newMasterPin = ""
                        confirmMasterPin = ""
                        showChangeMasterPinDialog = true
                    }
                )
            }

            // Decoy Vault PIN
            item {
                val hasDecoy = securityManager.hasDecoyPin()
                SettingActionCard(
                    title = "Decoy Vault PIN (Plausible Deniability)",
                    subtitle = if (hasDecoy) "Active: Typing decoy PIN opens fake harmless vault" else "Tap to configure decoy PIN for coercion protection",
                    icon = Icons.Default.Shield,
                    iconColor = VaultAmber,
                    onClick = {
                        newDecoyPin = ""
                        showDecoyPinDialog = true
                    }
                )
            }

            item {
                SectionHeader("STEALTH & DISGUISE")
            }

            // Calculator Disguise
            item {
                SettingSwitchCard(
                    title = "Calculator Stealth Mode",
                    subtitle = "Launches as a working calculator; unlocks only with secret PIN + '='",
                    icon = Icons.Default.Calculate,
                    iconColor = VaultCyan,
                    checked = calcDisguise,
                    onCheckedChange = {
                        calcDisguise = it
                        securityManager.isCalculatorDisguise = it
                        Toast.makeText(context, "Calculator stealth disguise updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                SectionHeader("PANIC LOCK & SENSORS")
            }

            // Flip to Lock
            item {
                SettingSwitchCard(
                    title = "Flip Down to Panic Lock",
                    subtitle = "Flipping phone face down immediately locks vault and exits",
                    icon = Icons.Default.ScreenRotation,
                    iconColor = VaultRed,
                    checked = flipLock,
                    onCheckedChange = {
                        flipLock = it
                        securityManager.isFlipToLockEnabled = it
                    }
                )
            }

            // Shake to Lock
            item {
                SettingSwitchCard(
                    title = "Shake to Panic Lock",
                    subtitle = "Shake phone vigorously to instantly close vault",
                    icon = Icons.Default.Vibration,
                    iconColor = VaultAmber,
                    checked = shakeLock,
                    onCheckedChange = {
                        shakeLock = it
                        securityManager.isShakeToLockEnabled = it
                    }
                )
            }

            // Screenshot Blocker
            item {
                SettingSwitchCard(
                    title = "Block Screenshots & Recents Preview",
                    subtitle = "Enforces FLAG_SECURE to prevent screen grab and spying",
                    icon = Icons.Default.NoPhotography,
                    iconColor = VaultEmerald,
                    checked = screenshotBlock,
                    onCheckedChange = {
                        screenshotBlock = it
                        securityManager.isScreenshotBlocked = it
                    }
                )
            }

            item {
                SectionHeader("BREAK-IN & INTRUDER DETECTION")
            }

            // Intruder Log Card
            item {
                val failedCount = securityManager.getFailedAttempts()
                SettingActionCard(
                    title = "Break-in Logs (${intruderLogs.size} recorded)",
                    subtitle = if (failedCount > 0) "Alert: $failedCount recent failed PIN attempts!" else "View timestamps of unauthorized access attempts",
                    icon = Icons.Default.Warning,
                    iconColor = if (failedCount > 0) VaultRed else VaultTextMuted,
                    onClick = {
                        intruderLogs = securityManager.getIntruderLogs()
                        showIntruderLogsDialog = true
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Change Master PIN Dialog
    if (showChangeMasterPinDialog) {
        AlertDialog(
            onDismissRequest = { showChangeMasterPinDialog = false },
            containerColor = VaultSurface,
            title = {
                Text("Change Master PIN", color = VaultTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMasterPin,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 8) newMasterPin = it },
                        label = { Text("New PIN (4-8 digits)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultEmerald,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmMasterPin,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 8) confirmMasterPin = it },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultEmerald,
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
                        if (newMasterPin.length >= 4 && newMasterPin == confirmMasterPin) {
                            securityManager.setupMasterPin(
                                newMasterPin,
                                securityManager.getSecurityQuestion(),
                                "default"
                            )
                            showChangeMasterPinDialog = false
                            Toast.makeText(context, "Master PIN changed successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "PINs must match and be at least 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultEmerald)
                ) {
                    Text("Save PIN", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeMasterPinDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    // Decoy PIN Dialog
    if (showDecoyPinDialog) {
        AlertDialog(
            onDismissRequest = { showDecoyPinDialog = false },
            containerColor = VaultSurface,
            title = {
                Text("Decoy Vault PIN", color = VaultTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "A Decoy PIN (e.g. 0000) opens a completely separate, realistic dummy vault with harmless items. If someone forces you to unlock, enter the decoy PIN.",
                        color = VaultTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDecoyPin,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 8) newDecoyPin = it },
                        label = { Text("Decoy PIN (e.g. 0000)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VaultAmber,
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
                        if (newDecoyPin.length >= 4) {
                            securityManager.setupDecoyPin(newDecoyPin)
                            showDecoyPinDialog = false
                            Toast.makeText(context, "Decoy PIN set! Enter $newDecoyPin= to open decoy.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Decoy PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultAmber)
                ) {
                    Text("Enable Decoy", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecoyPinDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    // Intruder Logs Dialog
    if (showIntruderLogsDialog) {
        AlertDialog(
            onDismissRequest = { showIntruderLogsDialog = false },
            containerColor = VaultSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = VaultRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Break-in Attempt Logs", color = VaultTextPrimary, fontSize = 18.sp)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (intruderLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No break-in attempts detected.", color = VaultTextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(intruderLogs) { log ->
                                val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(log.timestampMs))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Unauthorized Attempt #${log.attemptNumber}",
                                            color = VaultRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = dateStr,
                                            color = VaultTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        securityManager.clearIntruderLogs()
                        intruderLogs = emptyList()
                        showIntruderLogsDialog = false
                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaultRed)
                ) {
                    Text("Clear Logs", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntruderLogsDialog = false }) {
                    Text("Close", color = VaultTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = VaultEmerald,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SettingActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = VaultTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SettingSwitchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, color = VaultTextSecondary, fontSize = 11.sp)
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = iconColor,
                    uncheckedThumbColor = VaultTextMuted,
                    uncheckedTrackColor = VaultSurface
                )
            )
        }
    }
}
