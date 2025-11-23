package app.notedrop.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for NoteDrop.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class NoteDropApplication : Application()
