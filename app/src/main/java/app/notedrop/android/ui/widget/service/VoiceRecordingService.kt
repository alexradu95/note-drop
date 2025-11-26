package app.notedrop.android.ui.widget.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.notedrop.android.MainActivity
import app.notedrop.android.R
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.toUserMessage
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import app.notedrop.android.ui.widget.VoiceCaptureWidget
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject

/**
 * Foreground service for voice recording from widget
 *
 * Features:
 * - Runs in foreground with notification
 * - Updates widget with recording status
 * - Shows recording duration
 * - Saves audio file when stopped
 */
@AndroidEntryPoint
class VoiceRecordingService : Service() {

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var providerFactory: ProviderFactory

    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_recording"

        private var isRecording = false
        private var mediaRecorder: MediaRecorder? = null
        private var recordingFile: File? = null

        fun startRecording(context: Context, glanceId: GlanceId) {
            if (!isRecording) {
                val intent = Intent(context, VoiceRecordingService::class.java).apply {
                    action = "START_RECORDING"
                    putExtra("glance_id", glanceId.toString())
                }
                context.startForegroundService(intent)
            }
        }

        fun stopRecording(context: Context) {
            if (isRecording) {
                val intent = Intent(context, VoiceRecordingService::class.java).apply {
                    action = "STOP_RECORDING"
                }
                context.startService(intent)
            }
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var durationJob: Job? = null
    private var recordingDuration = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_RECORDING" -> {
                startForeground(NOTIFICATION_ID, createNotification("Recording..."))
                startRecordingInternal()
            }
            "STOP_RECORDING" -> {
                stopRecordingInternal()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecordingInternal() {
        if (isRecording) return

        try {
            // Create output file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            recordingFile = File(cacheDir, "voice_note_$timestamp.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true

            // Start duration timer
            startDurationTimer()

            Log.d(TAG, "Recording started: ${recordingFile!!.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopSelf()
        }
    }

    private fun stopRecordingInternal() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            durationJob?.cancel()

            // Save the recording file to note repository
            Log.d(TAG, "Recording stopped: ${recordingFile?.absolutePath}")

            recordingFile?.let { file ->
                serviceScope.launch {
                    saveVoiceNote(file)
                }
            }

            // Update widget to idle state
            serviceScope.launch {
                val glanceManager = GlanceAppWidgetManager(this@VoiceRecordingService)

                // Update InteractiveQuickCaptureWidget
                val interactiveIds = glanceManager.getGlanceIds(InteractiveQuickCaptureWidget::class.java)
                interactiveIds.forEach { glanceId ->
                    updateAppWidgetState(this@VoiceRecordingService, glanceId) { prefs ->
                        prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] = "idle"
                    }
                    InteractiveQuickCaptureWidget().update(this@VoiceRecordingService, glanceId)
                }

                // Update VoiceCaptureWidget
                val voiceIds = glanceManager.getGlanceIds(VoiceCaptureWidget::class.java)
                voiceIds.forEach { glanceId ->
                    updateAppWidgetState(this@VoiceRecordingService, glanceId) { prefs ->
                        prefs[VoiceCaptureWidget.RECORDING_STATUS_KEY] = "idle"
                        prefs[VoiceCaptureWidget.RECORDING_DURATION_KEY] = 0
                    }
                    VoiceCaptureWidget().update(this@VoiceRecordingService, glanceId)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }

    private fun startDurationTimer() {
        recordingDuration = 0
        durationJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                recordingDuration++

                // Update notification with duration
                val notification = createNotification(formatDuration(recordingDuration))
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Update widget with duration
                updateWidgetDuration(recordingDuration)
            }
        }
    }

    private fun updateWidgetDuration(duration: Int) {
        serviceScope.launch {
            val glanceManager = GlanceAppWidgetManager(this@VoiceRecordingService)

            // Update VoiceCaptureWidget
            val voiceWidgetIds = glanceManager.getGlanceIds(VoiceCaptureWidget::class.java)
            voiceWidgetIds.forEach { glanceId ->
                updateAppWidgetState(this@VoiceRecordingService, glanceId) { prefs ->
                    prefs[VoiceCaptureWidget.RECORDING_DURATION_KEY] = duration
                }
                VoiceCaptureWidget().update(this@VoiceRecordingService, glanceId)
            }

            // Update InteractiveQuickCaptureWidget if needed
            val interactiveWidgetIds = glanceManager.getGlanceIds(InteractiveQuickCaptureWidget::class.java)
            interactiveWidgetIds.forEach { glanceId ->
                updateAppWidgetState(this@VoiceRecordingService, glanceId) { prefs ->
                    prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] = "recording"
                }
                InteractiveQuickCaptureWidget().update(this@VoiceRecordingService, glanceId)
            }
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("Recording %d:%02d", minutes, secs)
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NoteDrop")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mic_voice)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows recording status"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Save voice recording to vault and create note
     */
    private suspend fun saveVoiceNote(tempFile: File) {
        try {
            // Get default vault
            val vault = vaultRepository.getDefaultVault().getOrElse { error ->
                Log.e(TAG, "Failed to get default vault: $error")
                val errorMsg = if (error is app.notedrop.android.domain.model.AppError) {
                    error.toUserMessage()
                } else {
                    error.toString()
                }
                showErrorNotification("Failed to get vault: $errorMsg")
                tempFile.delete()
                return
            }

            if (vault == null) {
                Log.w(TAG, "No default vault configured, voice note not saved")
                showErrorNotification("No default vault configured. Please set up a vault in Settings.")
                tempFile.delete()
                return
            }

            // Copy audio file to vault's configured attachments folder
            val audioFileName = tempFile.name
            val config = vault.providerConfig as? app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig
            val attachmentFolder = config?.attachmentsPath ?: "attachments"
            val audioRelativePath = "$attachmentFolder/$audioFileName"

            // Get vault root and create attachment folder
            val vaultUri = android.net.Uri.parse(config?.vaultPath)
            val vaultRoot = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, vaultUri)

            if (vaultRoot != null && vaultRoot.exists()) {
                Log.d(TAG, "Vault root found: ${vaultRoot.name}, using attachment folder: $attachmentFolder")

                // Find or create attachment folder
                val audioFolder = vaultRoot.findFile(attachmentFolder) ?: vaultRoot.createDirectory(attachmentFolder)

                if (audioFolder != null) {
                    Log.d(TAG, "Attachment folder ready: ${audioFolder.name}")

                    // Create audio file in vault
                    val audioFile = audioFolder.createFile("audio/mp4", audioFileName)

                    if (audioFile != null) {
                        Log.d(TAG, "Audio file created in vault: ${audioFile.name}")

                        // Copy temp file to vault
                        contentResolver.openOutputStream(audioFile.uri)?.use { outputStream ->
                            tempFile.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        Log.d(TAG, "Audio file saved to vault: $audioRelativePath")

                        // Create note with voice recording reference
                        val note = Note(
                            content = "Voice recording captured",
                            title = "Voice Note",
                            vaultId = vault.id,
                            tags = listOf("voice-note"),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                            voiceRecordingPath = audioRelativePath,
                            transcriptionStatus = app.notedrop.android.domain.model.TranscriptionStatus.PENDING
                        )

                        val result = noteRepository.createNote(note)

                        result.onSuccess { savedNote ->
                            Log.d(TAG, "Voice note saved to database: ${savedNote.id}")

                            // Sync to provider
                            val noteProvider = providerFactory.getProvider(vault.providerType)
                            if (noteProvider.isAvailable(vault)) {
                                val providerResult = noteProvider.saveNote(savedNote, vault)
                                providerResult.onSuccess { filePath ->
                                    Log.d(TAG, "Voice note synced to provider: $filePath")
                                    showSuccessNotification("Voice note saved to daily note successfully")
                                    noteRepository.updateNote(savedNote.copy(
                                        filePath = filePath,
                                        isSynced = true
                                    )).onFailure { updateError ->
                                        Log.e(TAG, "Failed to update note: $updateError")
                                    }
                                }.onFailure { providerError ->
                                    Log.e(TAG, "Failed to sync voice note to provider: $providerError")
                                    val errorMsg = if (providerError is app.notedrop.android.domain.model.AppError) {
                                        providerError.toUserMessage()
                                    } else {
                                        providerError.toString()
                                    }
                                    showErrorNotification("Saved to database but failed to sync: $errorMsg")
                                }
                            } else {
                                Log.w(TAG, "Provider not available, note saved to database only")
                                showSuccessNotification("Voice note saved to database (vault offline)")
                            }
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to save voice note to database: $error")
                            val errorMsg = if (error is app.notedrop.android.domain.model.AppError) {
                                error.toUserMessage()
                            } else {
                                error.toString()
                            }
                            showErrorNotification("Failed to save note to database: $errorMsg")
                        }
                    } else {
                        Log.e(TAG, "Failed to create audio file in attachment folder: $attachmentFolder")
                        showErrorNotification("Failed to create audio file in vault. Check permissions.")
                    }
                } else {
                    Log.e(TAG, "Failed to create or find attachment folder: $attachmentFolder in vault")
                    showErrorNotification("Failed to create attachment folder: $attachmentFolder")
                }
            } else {
                Log.e(TAG, "Vault root not accessible: vaultUri=$vaultUri, exists=${vaultRoot?.exists()}")
                showErrorNotification("Cannot access vault. Please check vault permissions in Settings.")
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save voice note", e)
            showErrorNotification("Failed to save voice note: ${e.message}")
            tempFile.delete()
        }
    }

    /**
     * Show error notification to user
     */
    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Recording Failed")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_mic_voice)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Show success notification to user
     */
    private fun showSuccessNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Note Saved")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_mic_voice)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
        serviceJob.cancel()
    }
}
