package app.notedrop.android.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.notedrop.android.ui.capture.QuickCaptureScreen
import app.notedrop.android.ui.settings.SettingsScreen
import app.notedrop.android.domain.model.CaptureType

/**
 * Navigation routes for the app.
 *
 * VAULT-ONLY ARCHITECTURE: QuickCapture is now the main screen (no home/note list).
 */
sealed class Screen(val route: String) {
    object QuickCapture : Screen("quick_capture")
    object Settings : Screen("settings")
}

/**
 * Main navigation host for NoteDrop.
 *
 * VAULT-ONLY: The app starts directly at QuickCapture (quick notes creator).
 * No home screen or note list - pure creation-focused experience.
 *
 * @param startWithCaptureType Optional capture type from widget launch
 */
@Composable
fun NoteDropNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.QuickCapture.route,
    startWithCaptureType: CaptureType? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.QuickCapture.route) {
            QuickCaptureScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNoteSaved = {
                    // Note saved, just stay on quick capture (ready for next note)
                    Log.d("NoteDropNavigation", "Note saved successfully")
                },
                initialCaptureType = startWithCaptureType
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
