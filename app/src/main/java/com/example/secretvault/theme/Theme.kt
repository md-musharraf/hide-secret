package com.example.secretvault.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultEmerald,
    onPrimary = Color.Black,
    primaryContainer = VaultEmerald.copy(alpha = 0.2f),
    onPrimaryContainer = VaultEmeraldLight,
    secondary = VaultCyan,
    onSecondary = Color.Black,
    secondaryContainer = VaultCyan.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    tertiary = VaultBlue,
    background = VaultBackground,
    onBackground = VaultTextPrimary,
    surface = VaultSurface,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultSurfaceVariant,
    onSurfaceVariant = VaultTextSecondary,
    error = VaultRed,
    onError = Color.White
)

@Composable
fun SecretVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultDarkColorScheme,
        typography = Typography,
        content = content
    )
}
