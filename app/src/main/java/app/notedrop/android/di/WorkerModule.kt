package app.notedrop.android.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.notedrop.android.domain.sync.SyncWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing WorkManager dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    /**
     * Provides WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Provides WorkManager Configuration.
     * This is used by HiltWorkerFactory for dependency injection in workers.
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Schedule periodic sync work.
     * This should be called from Application.onCreate()
     */
    fun scheduleSyncWork(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // WiFi or mobile data
            .setRequiresBatteryNotLow(true) // Don't run when battery is low
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SyncWorker.PERIODIC_SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // Use KEEP to avoid rescheduling if work is already scheduled
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}
