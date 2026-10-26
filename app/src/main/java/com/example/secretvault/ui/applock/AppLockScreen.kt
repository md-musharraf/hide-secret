package com.example.secretvault.ui.applock

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.secretvault.applock.AppLockManager
import com.example.secretvault.model.LockedAppInfo
import com.example.secretvault.model.VaultMode
import com.example.secretvault.theme.VaultAmber
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultBorder
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    vaultMode: VaultMode,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appLockManager = remember { AppLockManager.getInstance(context) }

    var isAccessibilityEnabled by remember { mutableStateOf(appLockManager.isAccessibilityServiceEnabled()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = appLockManager.isAccessibilityServiceEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var appList by remember { mutableStateOf<List<LockedAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Fake Crash Screen Preview Tester
    var showFakeCrashDialog by remember { mutableStateOf(false) }
    var testedAppName by remember { mutableStateOf("WhatsApp") }
    var showSecretUnlockHint by remember { mutableStateOf(false) }

    fun loadApps() {
        scope.launch {
            isLoading = true
            appList = appLockManager.getInstalledApps()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadApps()
    }

    val filtered = appList.filter {
        searchQuery.isBlank() ||
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
    }

    val lockedCount = appList.count { it.isLocked }

    Scaffold(
        topBar = {
            VaultTopAppBar(
                title = "App Locker",
                subtitle = "$lockedCount apps protected with fake crash",
                accentColor = VaultAmber,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Accessibility Service Protection Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityEnabled) VaultEmerald.copy(alpha = 0.12f) else VaultAmber.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isAccessibilityEnabled) VaultEmerald else VaultAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isAccessibilityEnabled) "Real-Time Protection Active" else "Background Protection Inactive",
                                color = if (isAccessibilityEnabled) VaultEmerald else VaultAmber,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Apps will be camouflaged when opened" else "Accessibility permission required to lock apps",
                                color = VaultTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (!isAccessibilityEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "To enable automatic fake crash locking when apps are launched from the home screen, turn ON 'Calculator Vault' in Accessibility Settings.",
                            color = VaultTextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Find 'Calculator Vault' and switch ON", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VaultAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Turn On Accessibility Service", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Explanatory Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VaultSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = VaultAmber, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fake Crash Screen Camouflage",
                            color = VaultTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When anyone tries to open a locked app, it mimics a system crash ('Unfortunately, App has stopped'). Intruder believes the app crashed! Long-press 'OK' to reveal secret unlock.",
                        color = VaultTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            testedAppName = "WhatsApp"
                            showFakeCrashDialog = true
                            showSecretUnlockHint = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VaultAmber.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = VaultAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Fake Crash Screen Preview", color = VaultAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search installed apps...", color = VaultTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VaultAmber,
                    unfocusedBorderColor = VaultBorder,
                    focusedContainerColor = VaultCardBackground,
                    unfocusedContainerColor = VaultCardBackground,
                    focusedTextColor = VaultTextPrimary,
                    unfocusedTextColor = VaultTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VaultAmber
                    )
                } else if (filtered.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = VaultTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Apps Found",
                            color = VaultTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppLockItemRow(
                                app = app,
                                onToggleLock = { locked ->
                                    appLockManager.toggleAppLock(app.packageName, locked)
                                    loadApps()
                                    Toast.makeText(
                                        context,
                                        if (locked) "Locked ${app.appName}" else "Unlocked ${app.appName}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onToggleFakeCrash = { fakeCrash ->
                                    appLockManager.toggleFakeCrash(app.packageName, fakeCrash)
                                    loadApps()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Fake Crash Dialog Preview
    if (showFakeCrashDialog) {
        AlertDialog(
            onDismissRequest = { showFakeCrashDialog = false },
            containerColor = Color(0xFF23272F),
            title = {
                Text("App Error", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Unfortunately, $testedAppName has stopped.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (showSecretUnlockHint) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VaultEmerald.copy(alpha = 0.2f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Secret bypass triggered! Vault unlock prompt will appear.",
                                color = VaultEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Regular tap closes like normal crash
                        showFakeCrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151))
                ) {
                    Text("OK", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSecretUnlockHint = true
                    }
                ) {
                    Text("Report", color = Color(0xFF9CA3AF))
                }
            }
        )
    }
}

@Composable
fun AppLockItemRow(
    app: LockedAppInfo,
    onToggleLock: (Boolean) -> Unit,
    onToggleFakeCrash: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked) VaultSurface else VaultCardBackground
        )
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
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (app.isLocked) VaultAmber.copy(alpha = 0.2f) else VaultTextMuted.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (app.isLocked) VaultAmber else VaultTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = app.appName,
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = app.packageName,
                        color = VaultTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Switch(
                checked = app.isLocked,
                onCheckedChange = onToggleLock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = VaultAmber,
                    uncheckedThumbColor = VaultTextMuted,
                    uncheckedTrackColor = VaultCardBackground
                )
            )
        }
    }
}
