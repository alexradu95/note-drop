package app.notedrop.android.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background worker to update widgets periodically
 *
 * Features:
 * - Updates widget content in the background
 * - Refreshes recent notes count
 * - Updates dynamic content (e.g., "Today's Notes: 5")
 * - Respects battery and network constraints
 *
 * Scheduled by WidgetUpdateScheduler
 */
class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Update all widget instances
            QuickCaptureWidget().updateAll(context)

            Result.success()
        } catch (e: Exception) {
            // Log error and retry
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "widget_update_worker"
    }
}
