package app.notedrop.android.ui.widget.action

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import app.notedrop.android.ui.widget.service.VoiceRecordingService

/**
 * Opens text input dialog for quick note creation
 */
class OpenTextInputAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("WidgetActions", "OpenTextInputAction triggered")

        // Launch text input activity
        val intent = Intent(context, TextInputActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("glance_id", glanceId.toString())
        }
        context.startActivity(intent)
    }
}

/**
 * Starts or stops voice recording
 */
class VoiceRecordAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("WidgetActions", "VoiceRecordAction triggered")

        // Get current recording status
        val prefs = updateAppWidgetState(context, glanceId) { prefs ->
            val currentStatus = prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] ?: "idle"

            if (currentStatus == "recording") {
                // Stop recording
                prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] = "idle"
                VoiceRecordingService.stopRecording(context)
            } else {
                // Start recording
                prefs[InteractiveQuickCaptureWidget.RECORDING_STATUS_KEY] = "recording"
                VoiceRecordingService.startRecording(context, glanceId)
            }
        }

        // Update widget
        InteractiveQuickCaptureWidget().update(context, glanceId)
    }
}

/**
 * VAULT-ONLY: Camera capture removed - not part of vault-only feature scope.
 * User chose: Quick text capture, Voice recording, Tags, All widgets.
 * Camera functionality was not selected.
 *
 * Kept as no-op for widget compatibility.
 */
class InstantCameraAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d("WidgetActions", "InstantCameraAction triggered (no-op - camera removed in vault-only)")
        // Camera service and TransparentCameraActivity removed in vault-only architecture
        // This action does nothing but prevents widget compilation errors
    }
}
