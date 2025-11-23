package app.notedrop.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.notedrop.android.navigation.NoteDropNavigation
import app.notedrop.android.ui.theme.NoteDropTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for NoteDrop.
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoteDropTheme {
                NoteDropNavigation()
            }
        }
    }
}