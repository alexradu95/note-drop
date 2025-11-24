package app.notedrop.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.notedrop.android.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SyncQueue entities.
 * Manages the retry queue for failed synchronization attempts.
 */
@Dao
interface SyncQueueDao {

    /**
     * Get all items in the sync queue.
     */
    @Query("SELECT * FROM sync_queue ORDER BY nextRetryAt ASC")
    suspend fun getAllQueueItems(): List<SyncQueueEntity>

    /**
     * Observe all items in the sync queue.
     */
    @Query("SELECT * FROM sync_queue ORDER BY nextRetryAt ASC")
    fun observeAllQueueItems(): Flow<List<SyncQueueEntity>>

    /**
     * Get items ready for retry (nextRetryAt <= current time).
     * Current time is passed as parameter.
     */
    @Query("SELECT * FROM sync_queue WHERE nextRetryAt <= :currentTimeMillis ORDER BY nextRetryAt ASC")
    suspend fun getItemsReadyForRetry(currentTimeMillis: Long): List<SyncQueueEntity>

    /**
     * Get items for a specific vault.
     */
    @Query("SELECT * FROM sync_queue WHERE vaultId = :vaultId ORDER BY nextRetryAt ASC")
    suspend fun getItemsForVault(vaultId: String): List<SyncQueueEntity>

    /**
     * Get items that have exceeded max retries.
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount >= 5 ORDER BY lastAttemptAt DESC")
    suspend fun getFailedItems(): List<SyncQueueEntity>

    /**
     * Get queue item for a specific note.
     */
    @Query("SELECT * FROM sync_queue WHERE noteId = :noteId")
    suspend fun getQueueItem(noteId: String): SyncQueueEntity?

    /**
     * Get count of items in queue.
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getQueueSize(): Int

    /**
     * Get count of items ready for retry.
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE nextRetryAt <= :currentTimeMillis")
    suspend fun getReadyForRetryCount(currentTimeMillis: Long): Int

    /**
     * Insert or update a queue item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncQueueEntity)

    /**
     * Insert or update multiple queue items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SyncQueueEntity>)

    /**
     * Delete a queue item by note ID.
     */
    @Query("DELETE FROM sync_queue WHERE noteId = :noteId")
    suspend fun delete(noteId: String)

    /**
     * Delete all queue items for a vault.
     */
    @Query("DELETE FROM sync_queue WHERE vaultId = :vaultId")
    suspend fun deleteForVault(vaultId: String)

    /**
     * Delete all items that have exceeded max retries.
     */
    @Query("DELETE FROM sync_queue WHERE retryCount >= 5")
    suspend fun deleteFailedItems()

    /**
     * Clear the entire queue.
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()

    /**
     * Reset retry count for a specific item (for manual retry).
     */
    @Query("UPDATE sync_queue SET retryCount = 0, nextRetryAt = :nextRetryAtMillis WHERE noteId = :noteId")
    suspend fun resetRetryCount(noteId: String, nextRetryAtMillis: Long)

    /**
     * Reset retry counts for all failed items.
     */
    @Query("UPDATE sync_queue SET retryCount = 0, nextRetryAt = :nextRetryAtMillis WHERE retryCount >= 5")
    suspend fun resetAllFailedItems(nextRetryAtMillis: Long)
}
