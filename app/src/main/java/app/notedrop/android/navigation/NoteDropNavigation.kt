package app.notedrop.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.notedrop.android.ui.capture.QuickCaptureScreen
import app.notedrop.android.ui.home.HomeScreen
import app.notedrop.android.ui.settings.SettingsScreen

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
 */
@Composable
fun NoteDropNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
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
                }
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
