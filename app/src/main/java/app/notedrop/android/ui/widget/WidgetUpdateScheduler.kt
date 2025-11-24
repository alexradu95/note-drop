package app.notedrop.android.ui.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules periodic widget updates
 *
 * Features:
 * - Updates widgets every 30 minutes
 * - Respects battery optimization
 * - Only updates when device is not in low battery state
 * - Uses WorkManager for reliable background execution
 */
object WidgetUpdateScheduler {

    /**
     * Schedule periodic widget updates
     *
     * @param context Application context
     */
    fun scheduleUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            repeatInterval = 30,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    /**
     * Cancel scheduled widget updates
     *
     * @param context Application context
     */
    fun cancelUpdate(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(WidgetUpdateWorker.WORK_NAME)
    }
}
