package app.notedrop.android.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.notedrop.android.ui.capture.QuickCaptureScreen
import app.notedrop.android.ui.home.HomeScreen
import app.notedrop.android.ui.settings.SettingsScreen
import app.notedrop.android.ui.widget.CaptureType

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object QuickCapture : Screen("quick_capture")
    object Settings : Screen("settings")
}

/**
 * Main navigation host for NoteDrop.
 *
 * @param startWithCaptureType Optional capture type from widget launch
 */
@Composable
fun NoteDropNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    startWithCaptureType: CaptureType? = null
) {
    // Navigate to quick capture if launched from widget
    LaunchedEffect(startWithCaptureType) {
        Log.d("NoteDropNavigation", "LaunchedEffect triggered with captureType: $startWithCaptureType")
        if (startWithCaptureType != null) {
            Log.d("NoteDropNavigation", "Navigating to QuickCapture with type: $startWithCaptureType")
            navController.navigate(Screen.QuickCapture.route) {
                // Clear back stack to prevent multiple home screens
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onQuickCaptureClick = {
                    navController.navigate(Screen.QuickCapture.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.QuickCapture.route) {
            QuickCaptureScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNoteSaved = {
                    navController.popBackStack()
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
