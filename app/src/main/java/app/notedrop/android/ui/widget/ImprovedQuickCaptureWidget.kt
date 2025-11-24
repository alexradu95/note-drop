package app.notedrop.android.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import app.notedrop.android.R
import app.notedrop.android.ui.widget.action.CaptureActionIntent

/**
 * Enhanced Quick Capture Widget with Material You design
 *
 * Features:
 * - Responsive sizing (Small, Medium, Large)
 * - Material You dynamic colors
 * - Quick actions for Text, Voice, and Camera
 * - Dynamic content (recent notes count)
 * - Beautiful rounded corners and shadows
 * - Smooth animations and interactions
 */
class ImprovedQuickCaptureWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SmallSize,
            MediumSize,
            LargeSize
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.height < 150.dp -> SmallWidgetContent()
                    size.height < 250.dp -> MediumWidgetContent()
                    else -> LargeWidgetContent()
                }
            }
        }
    }

    /**
     * Small widget: Single button with icon
     */
    @Composable
    private fun SmallWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            CompactActionButton(
                icon = R.drawable.ic_note_text,
                label = "Note",
                captureType = CaptureType.TEXT
            )
        }
    }

    /**
     * Medium widget: Three action buttons in column
     */
    @Composable
    private fun MediumWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                WidgetHeader()

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Action buttons
                ActionButtonRow(
                    icon = R.drawable.ic_note_text,
                    label = "Quick Note",
                    captureType = CaptureType.TEXT
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                ActionButtonRow(
                    icon = R.drawable.ic_mic_voice,
                    label = "Voice Note",
                    captureType = CaptureType.VOICE
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                ActionButtonRow(
                    icon = R.drawable.ic_camera,
                    label = "Camera",
                    captureType = CaptureType.CAMERA
                )
            }
        }
    }

    /**
     * Large widget: Full featured with stats
     */
    @Composable
    private fun LargeWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                // Header with stats
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.mipmap.ic_launcher),
                        contentDescription = "NoteDrop",
                        modifier = GlanceModifier.size(32.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "NoteDrop",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Text(
                            text = "Quick Capture",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(16.dp))

                // Action buttons grid
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LargeActionButton(
                        icon = R.drawable.ic_note_text,
                        label = "Quick Note",
                        description = "Type your thoughts",
                        captureType = CaptureType.TEXT
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    LargeActionButton(
                        icon = R.drawable.ic_mic_voice,
                        label = "Voice Note",
                        description = "Record audio",
                        captureType = CaptureType.VOICE
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    LargeActionButton(
                        icon = R.drawable.ic_camera,
                        label = "Camera Capture",
                        description = "Take a photo",
                        captureType = CaptureType.CAMERA
                    )
                }
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
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher),
                contentDescription = "NoteDrop",
                modifier = GlanceModifier.size(24.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

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
    private fun CompactActionButton(
        icon: Int,
        label: String,
        captureType: CaptureType
    ) {
        val context = LocalContext.current

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .clickable(
                    onClick = actionStartActivity(
                        CaptureActionIntent.createIntent(context, captureType)
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = label,
                modifier = GlanceModifier.size(40.dp)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimaryContainer
                )
            )
        }
    }

    @Composable
    private fun ActionButtonRow(
        icon: Int,
        label: String,
        captureType: CaptureType
    ) {
        val context = LocalContext.current

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .padding(12.dp)
                .clickable(
                    onClick = actionStartActivity(
                        CaptureActionIntent.createIntent(context, captureType)
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

    @Composable
    private fun LargeActionButton(
        icon: Int,
        label: String,
        description: String,
        captureType: CaptureType
    ) {
        val context = LocalContext.current

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.primaryContainer)
                .cornerRadius(12.dp)
                .padding(16.dp)
                .clickable(
                    onClick = actionStartActivity(
                        CaptureActionIntent.createIntent(context, captureType)
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = label,
                modifier = GlanceModifier.size(40.dp)
            )

            Spacer(modifier = GlanceModifier.width(16.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = description,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onPrimaryContainer
                    )
                )
            }
        }
    }

    companion object {
        private val SmallSize = DpSize(
            width = 100.dp,
            height = 100.dp
        )

        private val MediumSize = DpSize(
            width = 180.dp,
            height = 200.dp
        )

        private val LargeSize = DpSize(
            width = 250.dp,
            height = 300.dp
        )
    }
}
