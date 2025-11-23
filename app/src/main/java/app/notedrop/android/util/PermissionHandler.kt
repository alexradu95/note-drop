package app.notedrop.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission state for RECORD_AUDIO.
 */
sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
}

/**
 * Remember audio permission state and launcher.
 */
@Composable
fun rememberAudioPermissionState(
    onPermissionResult: (Boolean) -> Unit = {}
): AudioPermissionState {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(
            if (hasAudioPermission(context)) {
                PermissionState.Granted
            } else {
                PermissionState.Denied
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            permissionState = if (granted) {
                PermissionState.Granted
            } else {
                PermissionState.Denied
            }
            onPermissionResult(granted)
        }
    )

    return remember(permissionState, launcher) {
        AudioPermissionState(
            state = permissionState,
            requestPermission = {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }
}

/**
 * Audio permission state holder.
 */
data class AudioPermissionState(
    val state: PermissionState,
    val requestPermission: () -> Unit
) {
    val isGranted: Boolean
        get() = state is PermissionState.Granted
}

/**
 * Check if audio recording permission is granted.
 */
private fun hasAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}
