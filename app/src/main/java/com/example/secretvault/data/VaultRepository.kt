package com.example.secretvault.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.secretvault.cache.VaultMemoryCache
import com.example.secretvault.model.SecretNote
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultStats
import com.example.secretvault.model.VaultType
import com.example.secretvault.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.SecretKey

class VaultRepository(private val context: Context) {

    private val baseDir: File = File(context.filesDir, "vault_storage").apply {
        if (!exists()) mkdirs()
        // Put .nomedia file
        File(this, ".nomedia").createNewFile()
    }

    private val masterDir = File(baseDir, "master").apply { if (!exists()) mkdirs() }
    private val decoyDir = File(baseDir, "decoy").apply { if (!exists()) mkdirs() }

    companion object {
        @Volatile
        private var INSTANCE: VaultRepository? = null

        fun getInstance(context: Context): VaultRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getStorageDir(isDecoy: Boolean): File = if (isDecoy) decoyDir else masterDir

    private fun getCategoryDir(type: VaultType, isDecoy: Boolean): File {
        val folderName = when (type) {
            VaultType.PHOTO -> "photos"
            VaultType.VIDEO -> "videos"
            VaultType.AUDIO -> "audios"
            VaultType.DOCUMENT -> "docs"
            VaultType.NOTE -> "notes"
        }
        return File(getStorageDir(isDecoy), folderName).apply {
            if (!exists()) mkdirs()
            File(this, ".nomedia").createNewFile()
        }
    }

    private fun getTrashDir(isDecoy: Boolean): File {
        return File(getStorageDir(isDecoy), "trash").apply {
            if (!exists()) mkdirs()
            File(this, ".nomedia").createNewFile()
        }
    }

    private fun getIndexFile(isDecoy: Boolean): File {
        return File(getStorageDir(isDecoy), "vault_index.json")
    }

    private fun getNotesFile(isDecoy: Boolean): File {
        return File(getStorageDir(isDecoy), "vault_notes.json")
    }

    // ================= Item Operations =================

    suspend fun getItems(isDecoy: Boolean): List<VaultItem> = withContext(Dispatchers.IO) {
        val file = getIndexFile(isDecoy)
        if (!file.exists()) {
            if (isDecoy) {
                // Populate default decoy items if empty
                populateDefaultDecoy(isDecoy)
            } else {
                return@withContext emptyList<VaultItem>()
            }
        }
        try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VaultItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VaultItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        originalPath = if (obj.has("originalPath") && !obj.isNull("originalPath")) obj.getString("originalPath") else null,
                        encryptedFileName = obj.getString("encryptedFileName"),
                        vaultType = VaultType.valueOf(obj.getString("vaultType")),
                        fileSizeBytes = obj.getLong("fileSizeBytes"),
                        dateAddedMs = obj.getLong("dateAddedMs"),
                        mimeType = obj.optString("mimeType", "*/*"),
                        isDecoy = obj.optBoolean("isDecoy", isDecoy),
                        isInTrash = obj.optBoolean("isInTrash", false),
                        trashedDateMs = obj.optLong("trashedDateMs", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveItems(items: List<VaultItem>, isDecoy: Boolean) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("originalPath", item.originalPath)
                put("encryptedFileName", item.encryptedFileName)
                put("vaultType", item.vaultType.name)
                put("fileSizeBytes", item.fileSizeBytes)
                put("dateAddedMs", item.dateAddedMs)
                put("mimeType", item.mimeType)
                put("isDecoy", item.isDecoy)
                put("isInTrash", item.isInTrash)
                put("trashedDateMs", item.trashedDateMs)
            }
            array.put(obj)
        }
        getIndexFile(isDecoy).writeText(array.toString())
    }

    suspend fun importFile(
        uri: Uri,
        originalName: String,
        mimeType: String,
        type: VaultType,
        key: SecretKey,
        isDecoy: Boolean
    ): VaultItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val encFileName = "$id.enc"
        val targetDir = getCategoryDir(type, isDecoy)
        val targetFile = File(targetDir, encFileName)

        context.contentResolver.openInputStream(uri)?.use { inStream ->
            FileOutputStream(targetFile).use { outStream ->
                CryptoEngine.encryptStream(inStream, outStream, key)
            }
        } ?: throw IllegalStateException("Could not open input stream from URI: $uri")

        val item = VaultItem(
            id = id,
            name = originalName,
            originalPath = uri.toString(),
            encryptedFileName = encFileName,
            vaultType = type,
            fileSizeBytes = targetFile.length(),
            dateAddedMs = System.currentTimeMillis(),
            mimeType = mimeType,
            isDecoy = isDecoy,
            isInTrash = false
        )

        val currentList = getItems(isDecoy).toMutableList()
        currentList.add(0, item)
        saveItems(currentList, isDecoy)
        item
    }

    suspend fun importRawBytes(
        bytes: ByteArray,
        name: String,
        mimeType: String,
        type: VaultType,
        key: SecretKey,
        isDecoy: Boolean
    ): VaultItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val encFileName = "$id.enc"
        val targetDir = getCategoryDir(type, isDecoy)
        val targetFile = File(targetDir, encFileName)

        ByteArrayInputStream(bytes).use { inStream ->
            FileOutputStream(targetFile).use { outStream ->
                CryptoEngine.encryptStream(inStream, outStream, key)
            }
        }

        val item = VaultItem(
            id = id,
            name = name,
            originalPath = null,
            encryptedFileName = encFileName,
            vaultType = type,
            fileSizeBytes = targetFile.length(),
            dateAddedMs = System.currentTimeMillis(),
            mimeType = mimeType,
            isDecoy = isDecoy,
            isInTrash = false
        )

        val currentList = getItems(isDecoy).toMutableList()
        currentList.add(0, item)
        saveItems(currentList, isDecoy)
        item
    }

    suspend fun exportFile(
        item: VaultItem,
        destinationUri: Uri,
        key: SecretKey
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val encFile = getEncryptedFile(item)
            if (!encFile.exists()) return@withContext false

            FileInputStream(encFile).use { inStream ->
                context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                    CryptoEngine.decryptStream(inStream, outStream, key)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getEncryptedFile(item: VaultItem): File {
        val parentDir = if (item.isInTrash) {
            getTrashDir(item.isDecoy)
        } else {
            getCategoryDir(item.vaultType, item.isDecoy)
        }
        return File(parentDir, item.encryptedFileName)
    }

    suspend fun loadDecryptedBitmap(item: VaultItem, key: SecretKey): Bitmap? = withContext(Dispatchers.IO) {
        // Fast path: check memory cache first
        VaultMemoryCache.get(item.id)?.let { return@withContext it }

        try {
            val encFile = getEncryptedFile(item)
            if (!encFile.exists()) return@withContext null

            val outStream = ByteArrayOutputStream()
            FileInputStream(encFile).use { inStream ->
                CryptoEngine.decryptStream(inStream, outStream, key)
            }
            val decryptedBytes = outStream.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
            if (bitmap != null) {
                VaultMemoryCache.put(item.id, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadDecryptedText(item: VaultItem, key: SecretKey): String? = withContext(Dispatchers.IO) {
        try {
            val encFile = getEncryptedFile(item)
            if (!encFile.exists()) return@withContext null

            val outStream = ByteArrayOutputStream()
            FileInputStream(encFile).use { inStream ->
                CryptoEngine.decryptStream(inStream, outStream, key)
            }
            outStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createDecryptedTempMediaFile(item: VaultItem, key: SecretKey): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "vault_temp").apply { if (!exists()) mkdirs() }
        val ext = item.name.substringAfterLast('.', "tmp")
        val tempFile = File(cacheDir, "play_${item.id}.$ext")
        tempFile.deleteOnExit()

        val encFile = getEncryptedFile(item)
        FileInputStream(encFile).use { inStream ->
            FileOutputStream(tempFile).use { outStream ->
                CryptoEngine.decryptStream(inStream, outStream, key)
            }
        }
        tempFile
    }

    suspend fun clearDecryptedTempFiles() = withContext(Dispatchers.IO) {
        VaultMemoryCache.clear()
        val cacheDir = File(context.cacheDir, "vault_temp")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    suspend fun moveToTrash(item: VaultItem) = withContext(Dispatchers.IO) {
        VaultMemoryCache.remove(item.id)
        val currentItems = getItems(item.isDecoy).toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val srcFile = getEncryptedFile(item)
            val updated = item.copy(isInTrash = true, trashedDateMs = System.currentTimeMillis())
            val destFile = getEncryptedFile(updated)
            if (srcFile.exists()) {
                srcFile.renameTo(destFile)
            }
            currentItems[index] = updated
            saveItems(currentItems, item.isDecoy)
        }
    }

    suspend fun restoreFromTrash(item: VaultItem) = withContext(Dispatchers.IO) {
        val currentItems = getItems(item.isDecoy).toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val srcFile = getEncryptedFile(item)
            val updated = item.copy(isInTrash = false, trashedDateMs = 0L)
            val destFile = getEncryptedFile(updated)
            if (srcFile.exists()) {
                srcFile.renameTo(destFile)
            }
            currentItems[index] = updated
            saveItems(currentItems, item.isDecoy)
        }
    }

    suspend fun permanentlyDelete(item: VaultItem) = withContext(Dispatchers.IO) {
        VaultMemoryCache.remove(item.id)
        val file = getEncryptedFile(item)
        if (file.exists()) {
            // Shred file with zeroes
            try {
                FileOutputStream(file).use { it.write(ByteArray(file.length().coerceAtMost(4096).toInt())) }
            } catch (e: Exception) { /* ignore */ }
            file.delete()
        }
        val currentItems = getItems(item.isDecoy).filterNot { it.id == item.id }
        saveItems(currentItems, item.isDecoy)
    }

    suspend fun createSampleItem(
        type: VaultType,
        key: SecretKey,
        isDecoy: Boolean
    ): VaultItem? = withContext(Dispatchers.IO) {
        try {
            when (type) {
                VaultType.PHOTO -> {
                    val bitmap = Bitmap.createBitmap(360, 360, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.rgb(16, 185, 129)) // Emerald
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("Secret Vault Sample", 180f, 180f, paint)
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    importRawBytes(
                        bytes = stream.toByteArray(),
                        name = "sample_photo_${System.currentTimeMillis() % 10000}.png",
                        mimeType = "image/png",
                        type = VaultType.PHOTO,
                        key = key,
                        isDecoy = isDecoy
                    )
                }
                VaultType.DOCUMENT -> {
                    val docText = "Confidential Document\n\nSecurity: AES-256-GCM\nDate: 2026-09-07\nStatus: Securely encrypted in vault."
                    importRawBytes(
                        bytes = docText.toByteArray(Charsets.UTF_8),
                        name = "sample_document.txt",
                        mimeType = "text/plain",
                        type = VaultType.DOCUMENT,
                        key = key,
                        isDecoy = isDecoy
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ================= Notes Operations =================

    suspend fun getNotes(isDecoy: Boolean): List<SecretNote> = withContext(Dispatchers.IO) {
        val file = getNotesFile(isDecoy)
        if (!file.exists()) {
            if (isDecoy) {
                return@withContext listOf(
                    SecretNote(
                        id = UUID.randomUUID().toString(),
                        title = "Grocery List",
                        content = "- Milk & Eggs\n- Fresh Vegetables\n- Whole wheat bread\n- Green tea",
                        category = "Personal",
                        isDecoy = true
                    ),
                    SecretNote(
                        id = UUID.randomUUID().toString(),
                        title = "Team Meeting Notes",
                        content = "Discuss sprint roadmap, update design tickets, follow up on API endpoints.",
                        category = "General",
                        isDecoy = true
                    )
                )
            }
            return@withContext emptyList<SecretNote>()
        }
        try {
            val array = JSONArray(file.readText())
            val list = mutableListOf<SecretNote>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SecretNote(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        category = obj.optString("category", "General"),
                        dateCreatedMs = obj.getLong("dateCreatedMs"),
                        dateModifiedMs = obj.getLong("dateModifiedMs"),
                        isPinned = obj.optBoolean("isPinned", false),
                        isDecoy = obj.optBoolean("isDecoy", isDecoy)
                    )
                )
            }
            list.sortedWith(compareByDescending<SecretNote> { it.isPinned }.thenByDescending { it.dateModifiedMs })
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveNote(note: SecretNote, isDecoy: Boolean) = withContext(Dispatchers.IO) {
        val current = getNotes(isDecoy).toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index != -1) {
            current[index] = note.copy(dateModifiedMs = System.currentTimeMillis())
        } else {
            current.add(0, note)
        }

        val array = JSONArray()
        for (n in current) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("dateCreatedMs", n.dateCreatedMs)
                put("dateModifiedMs", n.dateModifiedMs)
                put("isPinned", n.isPinned)
                put("isDecoy", n.isDecoy)
            }
            array.put(obj)
        }
        getNotesFile(isDecoy).writeText(array.toString())
    }

    suspend fun deleteNote(id: String, isDecoy: Boolean) = withContext(Dispatchers.IO) {
        val current = getNotes(isDecoy).filterNot { it.id == id }
        val array = JSONArray()
        for (n in current) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("dateCreatedMs", n.dateCreatedMs)
                put("dateModifiedMs", n.dateModifiedMs)
                put("isPinned", n.isPinned)
                put("isDecoy", n.isDecoy)
            }
            array.put(obj)
        }
        getNotesFile(isDecoy).writeText(array.toString())
    }

    // ================= Stats =================

    suspend fun getStats(isDecoy: Boolean): VaultStats = withContext(Dispatchers.IO) {
        val items = getItems(isDecoy)
        val notes = getNotes(isDecoy)

        val active = items.filterNot { it.isInTrash }
        val photos = active.count { it.vaultType == VaultType.PHOTO }
        val videos = active.count { it.vaultType == VaultType.VIDEO }
        val audios = active.count { it.vaultType == VaultType.AUDIO }
        val docs = active.count { it.vaultType == VaultType.DOCUMENT }
        val trash = items.count { it.isInTrash }
        val totalBytes = active.sumOf { it.fileSizeBytes }

        VaultStats(
            photosCount = photos,
            videosCount = videos,
            audiosCount = audios,
            docsCount = docs,
            notesCount = notes.size,
            trashCount = trash,
            lockedAppsCount = 0,
            totalBytes = totalBytes
        )
    }

    private fun populateDefaultDecoy(isDecoy: Boolean) {
        // Will be populated when decoy mode is invoked
    }
}
