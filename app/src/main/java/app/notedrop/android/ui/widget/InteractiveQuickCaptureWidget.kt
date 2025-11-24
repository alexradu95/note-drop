package app.notedrop.android.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.notedrop.android.R
import app.notedrop.android.ui.widget.action.InstantCameraAction
import app.notedrop.android.ui.widget.action.OpenTextInputAction
import app.notedrop.android.ui.widget.action.VoiceRecordAction

/**
 * Interactive Quick Capture Widget with advanced features:
 *
 * - Text input directly on widget
 * - Voice recording with live status
 * - Instant camera capture
 *
 * Uses Glance interactive components and action callbacks
 */
class InteractiveQuickCaptureWidget : GlanceAppWidget() {

    companion object {
        private val SmallSize = DpSize(width = 120.dp, height = 120.dp)
        private val MediumSize = DpSize(width = 200.dp, height = 200.dp)
        private val LargeSize = DpSize(width = 300.dp, height = 300.dp)

        // State keys
        val WIDGET_TEXT_KEY = stringPreferencesKey("widget_text")
        val RECORDING_STATUS_KEY = stringPreferencesKey("recording_status")
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SmallSize, MediumSize, LargeSize)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.height < 150.dp -> SmallWidgetContent()
                    size.height < 250.dp -> MediumWidgetContent()
                    else -> LargeWidgetContent()
                }
            }
        }
    }

    @Composable
    private fun SmallWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            QuickNoteButton()
        }
    }

    @Composable
    private fun MediumWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Text input area
                QuickNoteInputArea()

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Action buttons row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceRecordButton()
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    CameraButton()
                }
            }
        }
    }

    @Composable
    private fun LargeWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                // Text input area with save button
                QuickNoteInputArea()

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Voice recording section
                VoiceRecordingSection()

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Camera capture button
                CameraSection()
            }
        }
    }

    @Composable
    private fun QuickNoteButton() {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .clickable(
                    onClick = actionRunCallback<OpenTextInputAction>()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_note_text),
                contentDescription = "Quick Note",
                modifier = GlanceModifier.size(40.dp)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Quick Note",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }

    @Composable
    private fun QuickNoteInputArea() {
        val widgetText = currentState(WIDGET_TEXT_KEY) ?: "Tap to add note..."

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(8.dp)
                .padding(12.dp)
                .clickable(onClick = actionRunCallback<OpenTextInputAction>())
        ) {
            Text(
                text = widgetText,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                ),
                maxLines = 3
            )
        }
    }

    @Composable
    private fun VoiceRecordButton() {
        val recordingStatus = currentState(RECORDING_STATUS_KEY) ?: "idle"
        val isRecording = recordingStatus == "recording"

        Box(
            modifier = GlanceModifier
                .size(56.dp)
                .background(
                    if (isRecording) GlanceTheme.colors.error
                    else GlanceTheme.colors.primaryContainer
                )
                .cornerRadius(28.dp)
                .clickable(onClick = actionRunCallback<VoiceRecordAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(
                    if (isRecording) R.drawable.ic_note_text // Will create stop icon
                    else R.drawable.ic_mic_voice
                ),
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                modifier = GlanceModifier.size(32.dp)
            )
        }
    }

    @Composable
    private fun CameraButton() {
        Box(
            modifier = GlanceModifier
                .size(56.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(28.dp)
                .clickable(onClick = actionRunCallback<InstantCameraAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_camera),
                contentDescription = "Instant Photo",
                modifier = GlanceModifier.size(32.dp)
            )
        }
    }

    @Composable
    private fun VoiceRecordingSection() {
        val recordingStatus = currentState(RECORDING_STATUS_KEY) ?: "idle"

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .padding(12.dp)
                .clickable(onClick = actionRunCallback<VoiceRecordAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_mic_voice),
                contentDescription = "Voice Recording",
                modifier = GlanceModifier.size(32.dp)
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = if (recordingStatus == "recording") "Recording..." else "Voice Note",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )

                if (recordingStatus == "recording") {
                    Text(
                        text = "Tap to stop",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun CameraSection() {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .padding(12.dp)
                .clickable(onClick = actionRunCallback<InstantCameraAction>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_camera),
                contentDescription = "Camera",
                modifier = GlanceModifier.size(32.dp)
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Instant Photo",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )

                Text(
                    text = "Tap to capture",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
    }
}
