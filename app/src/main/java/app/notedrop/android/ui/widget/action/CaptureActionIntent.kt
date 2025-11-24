package app.notedrop.android.ui.widget.action

import android.content.Context
import android.content.Intent
import app.notedrop.android.MainActivity
import app.notedrop.android.ui.widget.CaptureType

/**
 * Helper class to create intents for widget actions
 *
 * Provides methods to create properly configured intents for:
 * - Text capture
 * - Voice recording
 * - Camera capture
 */
object CaptureActionIntent {

    const val EXTRA_CAPTURE_TYPE = "extra_capture_type"
    const val EXTRA_WIDGET_LAUNCH = "extra_widget_launch"

    /**
     * Creates an intent to launch MainActivity with specific capture mode
     *
     * @param context Context
     * @param captureType Type of capture (TEXT, VOICE, CAMERA)
     * @return Configured intent
     */
    fun createIntent(context: Context, captureType: CaptureType): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_CAPTURE_TYPE, captureType.name)
            putExtra(EXTRA_WIDGET_LAUNCH, true)
        }
    }

    /**
     * Creates an intent for text capture
     */
    fun createTextCaptureIntent(context: Context): Intent {
        return createIntent(context, CaptureType.TEXT)
    }

    /**
     * Creates an intent for voice recording
     */
    fun createVoiceCaptureIntent(context: Context): Intent {
        return createIntent(context, CaptureType.VOICE)
    }

    /**
     * Creates an intent for camera capture
     */
    fun createCameraCaptureIntent(context: Context): Intent {
        return createIntent(context, CaptureType.CAMERA)
    }
}
