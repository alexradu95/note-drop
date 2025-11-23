package app.notedrop.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.dao.VaultDao
import app.notedrop.android.data.local.entity.NoteEntity
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
 */
@Database(
    entities = [
        NoteEntity::class,
        VaultEntity::class,
        TemplateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NoteDropDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun vaultDao(): VaultDao
    abstract fun templateDao(): TemplateDao

    companion object {
        const val DATABASE_NAME = "notedrop_database"
    }
}
