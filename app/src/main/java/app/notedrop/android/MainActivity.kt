package app.notedrop.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.notedrop.android.navigation.NoteDropNavigation
import app.notedrop.android.ui.theme.NoteDropTheme
import app.notedrop.android.domain.model.CaptureType
import app.notedrop.android.ui.widget.action.CaptureActionIntent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for NoteDrop.
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection.
 *
 * Handles:
 * - Standard app launches
 * - Widget launches with capture type
 * - Deep links and intents
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // Tracks the capture type requested from widget
    // Using mutableStateOf so changes trigger recomposition
    private var captureTypeFromWidget by mutableStateOf<CaptureType?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Check if launched from widget
        handleWidgetIntent(intent)

        enableEdgeToEdge()
        setContent {
            NoteDropTheme {
                NoteDropNavigation(
                    startWithCaptureType = captureTypeFromWidget
                )
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        Log.d(TAG, "onNewIntent called")
        setIntent(newIntent)

        // Handle the new intent and update state (triggers recomposition)
        handleWidgetIntent(newIntent)
    }

    /**
     * Handles intent from widget to determine capture type
     */
    private fun handleWidgetIntent(intentToHandle: Intent?) {
        intentToHandle?.let { intent ->
            val isWidgetLaunch = intent.getBooleanExtra(CaptureActionIntent.EXTRA_WIDGET_LAUNCH, false)
            val captureTypeString = intent.getStringExtra(CaptureActionIntent.EXTRA_CAPTURE_TYPE)

            Log.d(TAG, "handleWidgetIntent - isWidgetLaunch: $isWidgetLaunch, captureType: $captureTypeString")

            if (isWidgetLaunch && captureTypeString != null) {
                captureTypeFromWidget = try {
                    CaptureType.valueOf(captureTypeString)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid capture type: $captureTypeString", e)
                    null
                }

                Log.d(TAG, "Set captureTypeFromWidget to: $captureTypeFromWidget")
            }
        }
    }
}