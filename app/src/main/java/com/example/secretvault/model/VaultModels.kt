package com.example.secretvault.model

import java.io.Serializable

enum class VaultType {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT,
    NOTE
}

enum class VaultMode {
    LOCKED,
    MASTER,
    DECOY
}

data class VaultItem(
    val id: String,
    val name: String,
    val originalPath: String? = null,
    val encryptedFileName: String,
    val vaultType: VaultType,
    val fileSizeBytes: Long,
    val dateAddedMs: Long = System.currentTimeMillis(),
    val mimeType: String = "*/*",
    val isDecoy: Boolean = false,
    val isInTrash: Boolean = false,
    val trashedDateMs: Long = 0L
) : Serializable

data class SecretNote(
    val id: String,
    val title: String,
    val content: String,
    val category: String = "General",
    val dateCreatedMs: Long = System.currentTimeMillis(),
    val dateModifiedMs: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isDecoy: Boolean = false
) : Serializable

data class LockedAppInfo(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean = false,
    val fakeCrashEnabled: Boolean = true
)

data class IntruderLog(
    val id: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val failedPinEntered: String,
    val attemptNumber: Int = 1,
    val photoPath: String? = null
) : Serializable

data class VaultStats(
    val photosCount: Int = 0,
    val videosCount: Int = 0,
    val audiosCount: Int = 0,
    val docsCount: Int = 0,
    val notesCount: Int = 0,
    val trashCount: Int = 0,
    val lockedAppsCount: Int = 0,
    val totalBytes: Long = 0L
)
