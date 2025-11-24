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
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Foreground service for voice recording from widget
 *
 * Features:
 * - Runs in foreground with notification
 * - Updates widget with recording status
 * - Shows recording duration
 * - Saves audio file when stopped
 */
class VoiceRecordingService : Service() {

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

            // TODO: Save the recording file to note repository
            Log.d(TAG, "Recording stopped: ${recordingFile?.absolutePath}")

            // Update widget to idle state
            serviceScope.launch {
                val glanceManager = GlanceAppWidgetManager(this@VoiceRecordingService)
                val glanceIds = glanceManager.getGlanceIds(InteractiveQuickCaptureWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(this@VoiceRecordingService, glanceId) { prefs ->
                        prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] = "idle"
                    }
                    InteractiveQuickCaptureWidget().update(this@VoiceRecordingService, glanceId)
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecordingInternal()
        serviceJob.cancel()
    }
}
