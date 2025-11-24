package app.notedrop.android.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.notedrop.android.R
import app.notedrop.android.ui.widget.action.VoiceRecordAction

/**
 * Minimalistic 1x1 widget for voice recording
 * Shows live recording status when active
 */
class VoiceCaptureWidget : GlanceAppWidget() {

    companion object {
        val RECORDING_STATUS_KEY = stringPreferencesKey("voice_widget_recording_status")
        val RECORDING_DURATION_KEY = intPreferencesKey("voice_widget_recording_duration")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                VoiceCaptureContent()
            }
        }
    }

    @Composable
    private fun VoiceCaptureContent() {
        val recordingStatus = currentState(RECORDING_STATUS_KEY) ?: "idle"
        val recordingDuration = currentState(RECORDING_DURATION_KEY) ?: 0
        val isRecording = recordingStatus == "recording"

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    if (isRecording) GlanceTheme.colors.errorContainer
                    else GlanceTheme.colors.primaryContainer
                )
                .cornerRadius(16.dp)
                .clickable(onClick = actionRunCallback<VoiceRecordAction>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_mic_voice),
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                modifier = GlanceModifier.size(48.dp)
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (isRecording && recordingDuration > 0) {
                val minutes = recordingDuration / 60
                val seconds = recordingDuration % 60
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onErrorContainer
                    )
                )
            } else {
                Text(
                    text = if (isRecording) "Recording" else "Voice",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isRecording) GlanceTheme.colors.onErrorContainer
                        else GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
    }
}
