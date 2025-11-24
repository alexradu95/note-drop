package app.notedrop.android.ui.widget.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import app.notedrop.android.ui.widget.camera.TransparentCameraActivity

/**
 * Service helper for instant camera capture from widget
 *
 * Launches a transparent activity to capture photo instantly
 */
object CameraService {

    private const val TAG = "CameraService"

    /**
     * Initiates instant photo capture
     *
     * Opens a transparent activity that:
     * - Requests camera permission if needed
     * - Captures photo immediately
     * - Saves to note repository
     * - Closes automatically
     */
    fun capturePhoto(context: Context, glanceId: GlanceId) {
        Log.d(TAG, "capturePhoto triggered")

        val intent = Intent(context, TransparentCameraActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("glance_id", glanceId.toString())
        }

        context.startActivity(intent)
    }
}
