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
 * VAULT-ONLY: Removed background sync scheduling - notes write directly to vault.
 *
 * Initializes:
 * - Hilt dependency injection
 * - Widget update scheduling
 */
@HiltAndroidApp
class NoteDropApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic widget updates
        WidgetUpdateScheduler.scheduleUpdate(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
