package app.notedrop.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.dao.SyncStateDao
import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.dao.VaultDao
import app.notedrop.android.data.local.entity.NoteEntity
import app.notedrop.android.data.local.entity.SyncStateEntity
import app.notedrop.android.data.local.entity.TemplateEntity
import app.notedrop.android.data.local.entity.VaultEntity

/**
 * Room database for NoteDrop.
 *
 * This is the main database class that contains all entities and provides DAOs.
 *
 * @property noteDao DAO for note operations
 * @property vaultDao DAO for vault operations
 * @property templateDao DAO for template operations
 * @property syncStateDao DAO for sync state operations
 */
@Database(
    entities = [
        NoteEntity::class,
        VaultEntity::class,
        TemplateEntity::class,
        SyncStateEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NoteDropDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun vaultDao(): VaultDao
    abstract fun templateDao(): TemplateDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val DATABASE_NAME = "notedrop_database"

        /**
         * Migration from version 1 to 2: Add sync_states table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create sync_states table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_states (
                        noteId TEXT PRIMARY KEY NOT NULL,
                        vaultId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        localModifiedAt INTEGER NOT NULL,
                        remoteModifiedAt INTEGER,
                        lastSyncedAt INTEGER,
                        remotePath TEXT,
                        retryCount INTEGER NOT NULL,
                        errorMessage TEXT
                    )
                """.trimIndent())

                // Create indices for better query performance
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_sync_states_vaultId
                    ON sync_states(vaultId)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_sync_states_status
                    ON sync_states(status)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_sync_states_vaultId_status
                    ON sync_states(vaultId, status)
                """.trimIndent())
            }
        }

        /**
         * Migration from version 2 to 3: Add filePath column to notes table
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add filePath column to notes table
                database.execSQL("""
                    ALTER TABLE notes ADD COLUMN filePath TEXT
                """.trimIndent())
            }
        }
    }
}
