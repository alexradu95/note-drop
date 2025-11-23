package app.notedrop.android.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for recording voice notes.
 */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    /**
     * Start recording.
     */
    fun startRecording(): Result<String> {
        return try {
            // Create recordings directory
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }

            // Create recording file
            val timestamp = Instant.now().toEpochMilli()
            val file = File(recordingsDir, "recording_$timestamp.m4a")
            currentRecordingFile = file

            // Initialize MediaRecorder
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
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _recordingState.value = RecordingState.Recording(file.absolutePath)
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Stop recording.
     */
    fun stopRecording(): Result<String> {
        return try {
            val filePath = currentRecordingFile?.absolutePath
                ?: return Result.failure(IllegalStateException("No active recording"))

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _recordingState.value = RecordingState.Stopped(filePath)
            Result.success(filePath)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Cancel recording and delete the file.
     */
    fun cancelRecording(): Result<Unit> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            currentRecordingFile?.delete()
            currentRecordingFile = null

            _recordingState.value = RecordingState.Idle
            Result.success(Unit)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Pause recording (Android 24+).
     */
    fun pauseRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                _recordingState.value = RecordingState.Paused(
                    currentRecordingFile?.absolutePath ?: ""
                )
                Result.success(Unit)
            } else {
                Result.failure(UnsupportedOperationException("Pause not supported on this Android version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume recording (Android 24+).
     */
    fun resumeRecording(): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                _recordingState.value = RecordingState.Recording(
                    currentRecordingFile?.absolutePath ?: ""
                )
                Result.success(Unit)
            } else {
                Result.failure(UnsupportedOperationException("Resume not supported on this Android version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recording file size in bytes.
     */
    fun getRecordingSize(): Long {
        return currentRecordingFile?.length() ?: 0L
    }

    /**
     * Delete a recording file.
     */
    fun deleteRecording(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * State of the voice recorder.
 */
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val filePath: String) : RecordingState()
    data class Paused(val filePath: String) : RecordingState()
    data class Stopped(val filePath: String) : RecordingState()
    data class Error(val message: String) : RecordingState()
}
