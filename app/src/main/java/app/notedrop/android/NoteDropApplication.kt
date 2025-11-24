package app.notedrop.android

import android.app.Application
import app.notedrop.android.ui.widget.WidgetUpdateScheduler
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for NoteDrop.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 *
 * Initializes:
 * - Hilt dependency injection
 * - Widget update scheduling
 */
@HiltAndroidApp
class NoteDropApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic widget updates
        WidgetUpdateScheduler.scheduleUpdate(this)
    }
}
