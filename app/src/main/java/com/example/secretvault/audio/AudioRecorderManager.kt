package com.example.secretvault.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.secretvault.data.VaultRepository
import com.example.secretvault.model.VaultItem
import com.example.secretvault.model.VaultType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.SecretKey

data class RecordingState(
    val isRecording: Boolean = false,
    val durationSeconds: Int = 0
)

class AudioRecorderManager(
    private val context: Context,
    private val repository: VaultRepository
) {
    private var mediaRecorder: MediaRecorder? = null
    private var tempOutputFile: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState

    fun startRecording(): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "recordings").apply { if (!exists()) mkdirs() }
            tempOutputFile = File(cacheDir, "rec_${System.currentTimeMillis()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(tempOutputFile!!.absolutePath)
                prepare()
                start()
            }

            _recordingState.value = RecordingState(isRecording = true, durationSeconds = 0)
            startTimer()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording(null, false)
            false
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (isActive && _recordingState.value.isRecording) {
                delay(1000)
                seconds++
                _recordingState.value = _recordingState.value.copy(durationSeconds = seconds)
            }
        }
    }

    suspend fun stopAndSave(key: SecretKey, isDecoy: Boolean): VaultItem? = withContext(Dispatchers.IO) {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        _recordingState.value = RecordingState(isRecording = false, durationSeconds = 0)

        val file = tempOutputFile ?: return@withContext null
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return@withContext null
        }

        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val name = "Secret_Voice_$timeStr.m4a"

        val bytes = file.readBytes()
        file.delete()
        tempOutputFile = null

        repository.importRawBytes(
            bytes = bytes,
            name = name,
            mimeType = "audio/mp4",
            type = VaultType.AUDIO,
            key = key,
            isDecoy = isDecoy
        )
    }

    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaRecorder = null
        tempOutputFile?.delete()
        tempOutputFile = null
        _recordingState.value = RecordingState(isRecording = false, durationSeconds = 0)
    }

    private fun stopRecording(key: SecretKey?, isDecoy: Boolean) {
        timerJob?.cancel()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaRecorder = null
        tempOutputFile?.delete()
        tempOutputFile = null
        _recordingState.value = RecordingState(isRecording = false, durationSeconds = 0)
    }
}
