package app.notedrop.android.ui.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Widget receiver for Quick Capture Widget
 *
 * This receiver handles widget lifecycle events:
 * - Widget added to home screen
 * - Widget removed from home screen
 * - Widget update requests
 * - System broadcast events
 *
 * Extends GlanceAppWidgetReceiver to integrate with Android's widget system.
 */
class QuickCaptureWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ImprovedQuickCaptureWidget()
}
