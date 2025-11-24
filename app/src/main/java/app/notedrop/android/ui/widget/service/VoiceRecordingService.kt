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
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import app.notedrop.android.ui.widget.VoiceCaptureWidget
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
            val vault = vaultRepository.getDefaultVault()
            if (vault == null) {
                Log.w(TAG, "No default vault configured, voice note not saved")
                tempFile.delete()
                return
            }

            // Copy audio file to vault's audio/attachments folder
            val audioFileName = tempFile.name
            val audioRelativePath = "audio/$audioFileName"

            // Get vault root and create audio folder
            val vaultUri = android.net.Uri.parse((vault.providerConfig as? app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig)?.vaultPath)
            val vaultRoot = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, vaultUri)

            if (vaultRoot != null && vaultRoot.exists()) {
                // Find or create audio folder
                val audioFolder = vaultRoot.findFile("audio") ?: vaultRoot.createDirectory("audio")

                if (audioFolder != null) {
                    // Create audio file in vault
                    val audioFile = audioFolder.createFile("audio/m4a", audioFileName)

                    if (audioFile != null) {
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
                                    noteRepository.updateNote(savedNote.copy(
                                        filePath = filePath,
                                        isSynced = true
                                    ))
                                }.onFailure { providerError ->
                                    Log.e(TAG, "Failed to sync voice note to provider", providerError)
                                }
                            }
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to save voice note to database", error)
                        }
                    }
                }
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save voice note", e)
            tempFile.delete()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
        serviceJob.cancel()
    }
}
