package com.example.secretvault

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultMode
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.ui.applock.AppLockScreen
import com.example.secretvault.ui.audio.AudioVaultScreen
import com.example.secretvault.ui.calculator.CalculatorDisguiseScreen
import com.example.secretvault.ui.dashboard.VaultDashboardScreen
import com.example.secretvault.ui.document.DocumentVaultScreen
import com.example.secretvault.ui.media.MediaVaultScreen
import com.example.secretvault.ui.notes.SecretNotesScreen
import com.example.secretvault.ui.settings.SecuritySettingsScreen
import com.example.secretvault.ui.trash.RecycleBinScreen

sealed class VaultScreen {
    object Calculator : VaultScreen()
    object Dashboard : VaultScreen()
    object MediaVault : VaultScreen()
    object AudioVault : VaultScreen()
    object DocumentVault : VaultScreen()
    object SecretNotes : VaultScreen()
    object AppLock : VaultScreen()
    object SecuritySettings : VaultScreen()
    object RecycleBin : VaultScreen()
}

@Composable
fun MainNavigation(
    securityManager: SecurityManager,
    repository: VaultRepository
) {
    var vaultMode by remember { mutableStateOf(securityManager.currentMode) }
    val screenStack = remember { mutableStateListOf<VaultScreen>(VaultScreen.Calculator) }
    val currentScreen = screenStack.lastOrNull() ?: VaultScreen.Calculator

    fun navigateTo(screen: VaultScreen) {
        screenStack.add(screen)
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
        }
    }

    fun lockVault() {
        securityManager.lockVault()
        vaultMode = VaultMode.LOCKED
        screenStack.clear()
        screenStack.add(VaultScreen.Calculator)
    }

    BackHandler(enabled = screenStack.size > 1 || currentScreen != VaultScreen.Calculator) {
        if (screenStack.size > 1) {
            if (currentScreen == VaultScreen.Dashboard) {
                lockVault()
            } else {
                navigateBack()
            }
        } else if (currentScreen != VaultScreen.Calculator) {
            lockVault()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                is VaultScreen.Calculator -> {
                    CalculatorDisguiseScreen(
                        securityManager = securityManager,
                        onVaultUnlocked = { mode ->
                            vaultMode = mode
                            screenStack.clear()
                            screenStack.add(VaultScreen.Dashboard)
                        }
                    )
                }

                is VaultScreen.Dashboard -> {
                    VaultDashboardScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        repository = repository,
                        onNavigateToMedia = { navigateTo(VaultScreen.MediaVault) },
                        onNavigateToAudio = { navigateTo(VaultScreen.AudioVault) },
                        onNavigateToDocuments = { navigateTo(VaultScreen.DocumentVault) },
                        onNavigateToNotes = { navigateTo(VaultScreen.SecretNotes) },
                        onNavigateToAppLock = { navigateTo(VaultScreen.AppLock) },
                        onNavigateToSettings = { navigateTo(VaultScreen.SecuritySettings) },
                        onNavigateToTrash = { navigateTo(VaultScreen.RecycleBin) },
                        onLockVault = { lockVault() }
                    )
                }

                is VaultScreen.MediaVault -> {
                    MediaVaultScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        repository = repository,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.AudioVault -> {
                    AudioVaultScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        repository = repository,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.DocumentVault -> {
                    DocumentVaultScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        repository = repository,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.SecretNotes -> {
                    SecretNotesScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        repository = repository,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.AppLock -> {
                    AppLockScreen(
                        vaultMode = vaultMode,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.SecuritySettings -> {
                    SecuritySettingsScreen(
                        vaultMode = vaultMode,
                        securityManager = securityManager,
                        onNavigateBack = { navigateBack() }
                    )
                }

                is VaultScreen.RecycleBin -> {
                    RecycleBinScreen(
                        vaultMode = vaultMode,
                        repository = repository,
                        onNavigateBack = { navigateBack() }
                    )
                }
            }
        }
    }
}
