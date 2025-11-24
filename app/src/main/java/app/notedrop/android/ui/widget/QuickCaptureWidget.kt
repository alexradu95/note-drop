package app.notedrop.android.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import app.notedrop.android.MainActivity
import app.notedrop.android.R

/**
 * Quick Capture Widget for NoteDrop
 *
 * Provides quick access to:
 * - Text input (opens app with quick capture)
 * - Voice recording (opens app with voice mode)
 * - Camera capture (opens app with camera mode)
 *
 * Features:
 * - Material You dynamic theming
 * - Multiple size modes (Small, Medium, Large)
 * - Responsive layouts
 * - Quick action buttons
 */
class QuickCaptureWidget : GlanceAppWidget() {

    /**
     * Widget supports multiple size modes for different launcher configurations
     */
    override val sizeMode = SizeMode.Responsive(
        setOf(
            SMALL_SIZE,
            MEDIUM_SIZE,
            LARGE_SIZE
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickCaptureWidgetContent()
            }
        }
    }

    @Composable
    private fun QuickCaptureWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                WidgetHeader()

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Quick action buttons
                QuickActionButtons()
            }
        }
    }

    @Composable
    private fun WidgetHeader() {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            // App icon
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = "NoteDrop",
                modifier = GlanceModifier.size(24.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // App name
            Text(
                text = "NoteDrop",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
        }
    }

    @Composable
    private fun QuickActionButtons() {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Text input button
            ActionButton(
                icon = R.drawable.ic_launcher_foreground, // Placeholder - will create proper icons
                label = "Quick Note",
                captureType = CaptureType.TEXT
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Voice recording button
            ActionButton(
                icon = R.drawable.ic_launcher_foreground, // Placeholder - will create proper icons
                label = "Voice Note",
                captureType = CaptureType.VOICE
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Camera button
            ActionButton(
                icon = R.drawable.ic_launcher_foreground, // Placeholder - will create proper icons
                label = "Camera Note",
                captureType = CaptureType.CAMERA
            )
        }
    }

    @Composable
    private fun ActionButton(
        icon: Int,
        label: String,
        captureType: CaptureType
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp)
                .clickable(
                    onClick = actionStartActivity(
                        createLaunchIntent(captureType)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = label,
                modifier = GlanceModifier.size(32.dp)
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }

    /**
     * Creates an intent to launch MainActivity with specific capture mode
     */
    private fun createLaunchIntent(captureType: CaptureType): Intent {
        return Intent().apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_CAPTURE_TYPE, captureType.name)
        }
    }

    companion object {
        // Widget sizes
        private val SMALL_SIZE = DpSize(
            width = 120.dp,
            height = 120.dp
        )
        private val MEDIUM_SIZE = DpSize(
            width = 200.dp,
            height = 200.dp
        )
        private val LARGE_SIZE = DpSize(
            width = 300.dp,
            height = 300.dp
        )

        // Intent extras
        const val EXTRA_CAPTURE_TYPE = "extra_capture_type"
    }
}

/**
 * Enum representing different capture types
 */
enum class CaptureType {
    TEXT,
    VOICE,
    CAMERA
}
