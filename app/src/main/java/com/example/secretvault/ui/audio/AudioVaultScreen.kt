package com.example.secretvault.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.secretvault.audio.AudioPlayerManager
import com.example.secretvault.audio.AudioRecorderManager
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultMode
import com.example.secretvault.model.VaultType
import com.example.secretvault.security.SecurityManager
import com.example.secretvault.theme.VaultBackground
import com.example.secretvault.theme.VaultCardBackground
import com.example.secretvault.theme.VaultCyan
import com.example.secretvault.theme.VaultEmerald
import com.example.secretvault.theme.VaultRed
import com.example.secretvault.theme.VaultSurface
import com.example.secretvault.theme.VaultTextMuted
import com.example.secretvault.theme.VaultTextPrimary
import com.example.secretvault.theme.VaultTextSecondary
import com.example.secretvault.ui.common.VaultEmptyState
import com.example.secretvault.ui.common.VaultTopAppBar
import com.example.secretvault.ui.media.getFileNameFromUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVaultScreen(
    vaultMode: VaultMode,
    securityManager: SecurityManager,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDecoy = vaultMode == VaultMode.DECOY
    val key = securityManager.activeSecretKey

    val playerManager = remember { AudioPlayerManager(context, repository) }
    val recorderManager = remember { AudioRecorderManager(context, repository) }

    val playbackState by playerManager.playbackState.collectAsState()
    val recordingState by recorderManager.recordingState.collectAsState()

    var audioItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshList() {
        scope.launch {
            isLoading = true
            val all = repository.getItems(isDecoy)
            audioItems = all.filter { !it.isInTrash && it.vaultType == VaultType.AUDIO }
            isLoading = false
        }
    }

    LaunchedEffect(vaultMode) {
        refreshList()
    }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.release()
            recorderManager.cancelRecording()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty() && key != null) {
            scope.launch {
                isLoading = true
                var count = 0
                for (uri in uris) {
                    try {
                        val name = getFileNameFromUri(context, uri) ?: "audio_${System.currentTimeMillis()}.mp3"
                        val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
                        repository.importFile(uri, name, mime, VaultType.AUDIO, key, isDecoy)
                        count++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                isLoading = false
                refreshList()
                Toast.makeText(context, "Encrypted & imported $count audio files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = recorderManager.startRecording()
            if (started) {
                Toast.makeText(context, "Recording secret memo...", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice memos", Toast.LENGTH_LONG).show()
        }
    }

    fun addSampleAudio() {
        if (key == null) return
        scope.launch {
            isLoading = true
            try {
                val sampleWav = createSampleWavBytes()
                repository.importRawBytes(
                    bytes = sampleWav,
                    name = "Secret_Audio_Memo_${System.currentTimeMillis() % 10000}.wav",
                    mimeType = "audio/wav",
                    type = VaultType.AUDIO,
                    key = key,
                    isDecoy = isDecoy
                )
                refreshList()
                Toast.makeText(context, "Encrypted sample memo created!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            VaultTopAppBar(
                title = "Secret Audios",
                subtitle = "${audioItems.size} encrypted recordings",
                accentColor = VaultCyan,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(onClick = { addSampleAudio() }) {
                        Text("+ Sample", color = VaultCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mic button for recording secret memo
                FloatingActionButton(
                    onClick = {
                        if (recordingState.isRecording && key != null) {
                            scope.launch {
                                val item = recorderManager.stopAndSave(key, isDecoy)
                                if (item != null) {
                                    refreshList()
                                    Toast.makeText(context, "Encrypted voice memo saved!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                val started = recorderManager.startRecording()
                                if (started) {
                                    Toast.makeText(context, "Recording secret memo...", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    containerColor = if (recordingState.isRecording) VaultRed else VaultSurface,
                    contentColor = if (recordingState.isRecording) Color.White else VaultCyan,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (recordingState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record Memo",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Plus button for importing audio files
                FloatingActionButton(
                    onClick = { audioPicker.launch("audio/*") },
                    containerColor = VaultCyan,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import Audio", modifier = Modifier.size(26.dp))
                }
            }
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Recording Active Bar
            AnimatedVisibility(visible = recordingState.isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultRed.copy(alpha = 0.15f))
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
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(VaultRed)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Recording Secret Memo: ${formatDuration(recordingState.durationSeconds * 1000)}",
                                color = VaultRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { recorderManager.cancelRecording() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = VaultTextSecondary)
                        }
                    }
                }
            }

            // Audio List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VaultCyan
                    )
                } else if (audioItems.isEmpty()) {
                    VaultEmptyState(
                        icon = Icons.Default.Audiotrack,
                        title = "No Secret Audio Files",
                        description = "Record secret voice memos directly or import audio files into military-grade encryption.",
                        actionButtonText = "Import Audio",
                        onActionClick = { audioPicker.launch("audio/*") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(audioItems, key = { it.id }) { item ->
                            val isCurrentlyPlaying = playbackState.currentItemId == item.id && playbackState.isPlaying
                            AudioItemRow(
                                item = item,
                                isPlaying = isCurrentlyPlaying,
                                onClick = {
                                    if (key != null) {
                                        if (playbackState.currentItemId == item.id) {
                                            playerManager.togglePlayPause()
                                        } else {
                                            playerManager.play(item, key)
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        if (playbackState.currentItemId == item.id) {
                                            playerManager.stop()
                                        }
                                        repository.moveToTrash(item)
                                        refreshList()
                                        Toast.makeText(context, "Audio moved to Recycle Bin", Toast.LENGTH_SHORT).show()
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

            // Bottom Audio Player Bar
            AnimatedVisibility(visible = playbackState.currentItemId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .background(VaultCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = VaultCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = playbackState.currentItemName ?: "Audio",
                                        color = VaultTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${formatDuration(playbackState.currentPositionMs)} / ${formatDuration(playbackState.durationMs)}",
                                        color = VaultTextSecondary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { playerManager.togglePlayPause() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(VaultCyan)
                                ) {
                                    Icon(
                                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black
                                    )
                                }
                                IconButton(onClick = { playerManager.stop() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Player", tint = VaultTextMuted)
                                }
                            }
                        }

                        // Progress Slider
                        if (playbackState.durationMs > 0) {
                            Slider(
                                value = playbackState.currentPositionMs.toFloat(),
                                onValueChange = { playerManager.seekTo(it.toInt()) },
                                valueRange = 0f..playbackState.durationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = VaultCyan,
                                    activeTrackColor = VaultCyan,
                                    inactiveTrackColor = VaultCyan.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioItemRow(
    item: VaultItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) VaultCyan else VaultCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) Color.Black else VaultCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.name,
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.dateAddedMs))
                    val sizeKb = item.fileSizeBytes / 1024
                    Text(
                        text = "$dateStr • ${sizeKb}KB",
                        color = VaultTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = VaultRed.copy(alpha = 0.7f))
            }
        }
    }
}

fun formatDuration(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun createSampleWavBytes(): ByteArray {
    val sampleRate = 8000
    val durationSeconds = 2
    val numSamples = sampleRate * durationSeconds
    val dataSize = numSamples * 2 // 16-bit mono
    val totalSize = 36 + dataSize
    val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

    // RIFF chunk
    buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
    buffer.putInt(totalSize)
    buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

    // fmt chunk
    buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
    buffer.putInt(16) // Subchunk1Size (16 for PCM)
    buffer.putShort(1.toShort()) // AudioFormat (1 for PCM)
    buffer.putShort(1.toShort()) // NumChannels (1 mono)
    buffer.putInt(sampleRate) // SampleRate
    buffer.putInt(sampleRate * 2) // ByteRate
    buffer.putShort(2.toShort()) // BlockAlign
    buffer.putShort(16.toShort()) // BitsPerSample

    // data chunk
    buffer.put("data".toByteArray(Charsets.US_ASCII))
    buffer.putInt(dataSize)

    // Generate clean tone (440 Hz)
    val freq = 440.0
    for (i in 0 until numSamples) {
        val sample = (Math.sin(2.0 * Math.PI * i * freq / sampleRate) * 12000).toInt().toShort()
        buffer.putShort(sample)
    }
    return buffer.array()
}
