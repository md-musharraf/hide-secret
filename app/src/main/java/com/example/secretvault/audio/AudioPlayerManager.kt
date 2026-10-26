package com.example.secretvault.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.crypto.SecretKey

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val currentItemId: String? = null,
    val currentItemName: String? = null
)

class AudioPlayerManager(
    private val context: Context,
    private val repository: VaultRepository
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTempFile: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState

    fun play(item: VaultItem, key: SecretKey) {
        scope.launch {
            try {
                stop()
                currentTempFile = repository.createDecryptedTempMediaFile(item, key)

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(currentTempFile!!.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            currentPositionMs = 0
                        )
                        stopProgressTracking()
                    }
                }

                _playbackState.value = AudioPlaybackState(
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = mediaPlayer?.duration ?: 0,
                    currentItemId = item.id,
                    currentItemName = item.name
                )
                startProgressTracking()
            } catch (e: Exception) {
                e.printStackTrace()
                stop()
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopProgressTracking()
            } else {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracking()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun stop() {
        stopProgressTracking()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaPlayer = null

        currentTempFile?.delete()
        currentTempFile = null

        _playbackState.value = AudioPlaybackState()
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val pos = mediaPlayer?.currentPosition ?: 0
                val dur = mediaPlayer?.duration ?: 0
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = pos,
                    durationMs = dur
                )
                delay(300)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stop()
    }
}
