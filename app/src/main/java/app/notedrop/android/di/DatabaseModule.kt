package app.notedrop.android.di

import android.content.Context
import androidx.room.Room
import app.notedrop.android.data.local.NoteDropDatabase
import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.dao.SyncStateDao
import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.dao.VaultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideNoteDropDatabase(
        @ApplicationContext context: Context
    ): NoteDropDatabase {
        return Room.databaseBuilder(
            context,
            NoteDropDatabase::class.java,
            NoteDropDatabase.DATABASE_NAME
        )
            .addMigrations(NoteDropDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    /**
     * Provides NoteDao from the database.
     */
    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDropDatabase): NoteDao {
        return database.noteDao()
    }

    /**
     * Provides VaultDao from the database.
     */
    @Provides
    @Singleton
    fun provideVaultDao(database: NoteDropDatabase): VaultDao {
        return database.vaultDao()
    }

    /**
     * Provides TemplateDao from the database.
     */
    @Provides
    @Singleton
    fun provideTemplateDao(database: NoteDropDatabase): TemplateDao {
        return database.templateDao()
    }

    /**
     * Provides SyncStateDao from the database.
     */
    @Provides
    @Singleton
    fun provideSyncStateDao(database: NoteDropDatabase): SyncStateDao {
        return database.syncStateDao()
    }
}
