package app.notedrop.android.ui.widget.action

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import app.notedrop.android.R
import app.notedrop.android.domain.model.toUserMessage
import app.notedrop.android.domain.usecase.CreateNoteUseCase
import app.notedrop.android.ui.theme.NoteDropTheme
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog-style activity for quick text input from widget
 *
 * Features:
 * - Appears as overlay/dialog
 * - Simple text input
 * - Quick save button
 * - Closes automatically after save
 */
@AndroidEntryPoint
class TextInputActivity : ComponentActivity() {

    @Inject
    lateinit var createNoteUseCase: CreateNoteUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channel for feedback
        createNotificationChannel()

        // Make activity appear as dialog
        setFinishOnTouchOutside(true)

        setContent {
            NoteDropTheme {
                var text by remember { mutableStateOf("") }
                var isSaving by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Quick Note",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    placeholder = { Text("Type your note here...") },
                                    maxLines = 8,
                                    enabled = !isSaving
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { finish() },
                                        enabled = !isSaving
                                    ) {
                                        Text("Cancel")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            if (text.isNotBlank()) {
                                                isSaving = true
                                                saveNote(text)
                                            }
                                        },
                                        enabled = text.isNotBlank() && !isSaving
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveNote(content: String) {
        lifecycleScope.launch {
            try {
                // Show saving notification
                showNotification(
                    title = "Saving Note",
                    message = "Saving your note...",
                    notificationId = NOTIFICATION_ID_SAVING
                )

                // Use the unified CreateNoteUseCase
                val result = createNoteUseCase(content = content)

                result.onSuccess { savedNote ->
                    android.util.Log.d("TextInputActivity", "Note saved successfully: ${savedNote.id}")

                    // Show success notification
                    showNotification(
                        title = "Note Saved",
                        message = "Your note has been saved successfully",
                        notificationId = NOTIFICATION_ID_SUCCESS,
                        autoCancel = true
                    )

                    // Update widget to show saved text (preview) and refresh
                    val glanceManager = GlanceAppWidgetManager(this@TextInputActivity)
                    val glanceIds = glanceManager.getGlanceIds(InteractiveQuickCaptureWidget::class.java)

                    glanceIds.forEach { glanceId ->
                        updateAppWidgetState(this@TextInputActivity, glanceId) { prefs ->
                            prefs[InteractiveQuickCaptureWidget.WIDGET_TEXT_KEY] =
                                content.take(50) + if (content.length > 50) "..." else ""
                        }
                        // Force widget update to refresh UI
                        InteractiveQuickCaptureWidget().update(this@TextInputActivity, glanceId)
                    }
                }.onFailure { error ->
                    android.util.Log.e("TextInputActivity", "Failed to save note: $error")

                    // Show error notification
                    showNotification(
                        title = "Note Save Failed",
                        message = error.toUserMessage(),
                        notificationId = NOTIFICATION_ID_ERROR,
                        autoCancel = true,
                        isError = true
                    )
                }

                // Close activity after a short delay for user to see notification
                kotlinx.coroutines.delay(500)
                finish()
            } catch (e: Exception) {
                android.util.Log.e("TextInputActivity", "Exception during save", e)

                showNotification(
                    title = "Error",
                    message = "An unexpected error occurred: ${e.message}",
                    notificationId = NOTIFICATION_ID_ERROR,
                    autoCancel = true,
                    isError = true
                )

                kotlinx.coroutines.delay(500)
                finish()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Feedback",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for widget actions"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        notificationId: Int,
        autoCancel: Boolean = false,
        isError: Boolean = false
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(if (isError) R.drawable.ic_note_text else R.drawable.ic_note_text)
            .setPriority(if (isError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(autoCancel)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val CHANNEL_ID = "widget_feedback_channel"
        private const val NOTIFICATION_ID_SAVING = 1001
        private const val NOTIFICATION_ID_SUCCESS = 1002
        private const val NOTIFICATION_ID_ERROR = 1003
    }
}
