package app.notedrop.android.data.local

import androidx.room.TypeConverter
import app.notedrop.android.domain.model.SyncStatus

/**
 * Room type converters for custom types.
 */
class Converters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // Default to NEVER_SYNCED if unknown status
            SyncStatus.NEVER_SYNCED
        }
    }
}
