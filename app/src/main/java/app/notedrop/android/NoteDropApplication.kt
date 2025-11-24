package app.notedrop.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import app.notedrop.android.di.WorkerModule
import app.notedrop.android.ui.widget.WidgetUpdateScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for NoteDrop.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 *
 * Initializes:
 * - Hilt dependency injection
 * - Widget update scheduling
 * - Background sync scheduling
 */
@HiltAndroidApp
class NoteDropApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic widget updates
        WidgetUpdateScheduler.scheduleUpdate(this)

        // Schedule periodic sync work
        WorkerModule.scheduleSyncWork(workManager)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
