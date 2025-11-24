package app.notedrop.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.notedrop.android.data.local.entity.SyncStateEntity
import app.notedrop.android.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SyncState entities.
 * Provides methods to track synchronization state for notes.
 */
@Dao
interface SyncStateDao {

    /**
     * Get sync state for a specific note.
     */
    @Query("SELECT * FROM sync_states WHERE noteId = :noteId")
    suspend fun getSyncState(noteId: String): SyncStateEntity?

    /**
     * Get sync state for a specific note as Flow for reactive updates.
     */
    @Query("SELECT * FROM sync_states WHERE noteId = :noteId")
    fun observeSyncState(noteId: String): Flow<SyncStateEntity?>

    /**
     * Get all sync states for a vault.
     */
    @Query("SELECT * FROM sync_states WHERE vaultId = :vaultId")
    fun getSyncStatesForVault(vaultId: String): Flow<List<SyncStateEntity>>

    /**
     * Get all sync states with a specific status.
     */
    @Query("SELECT * FROM sync_states WHERE status = :status")
    suspend fun getByStatus(status: SyncStatus): List<SyncStateEntity>

    /**
     * Get all sync states with a specific status for a vault.
     */
    @Query("SELECT * FROM sync_states WHERE vaultId = :vaultId AND status = :status")
    suspend fun getByStatusForVault(vaultId: String, status: SyncStatus): List<SyncStateEntity>

    /**
     * Get notes that need to be uploaded (pending upload or error with retries remaining).
     */
    @Query("""
        SELECT * FROM sync_states
        WHERE vaultId = :vaultId
        AND (status = 'PENDING_UPLOAD' OR (status = 'ERROR' AND retryCount < :maxRetries))
        ORDER BY localModifiedAt ASC
    """)
    suspend fun getPendingUploads(vaultId: String, maxRetries: Int = 3): List<SyncStateEntity>

    /**
     * Get notes that need to be downloaded.
     */
    @Query("""
        SELECT * FROM sync_states
        WHERE vaultId = :vaultId
        AND status = 'PENDING_DOWNLOAD'
        ORDER BY remoteModifiedAt ASC
    """)
    suspend fun getPendingDownloads(vaultId: String): List<SyncStateEntity>

    /**
     * Get notes with conflicts.
     */
    @Query("""
        SELECT * FROM sync_states
        WHERE vaultId = :vaultId
        AND status = 'CONFLICT'
        ORDER BY localModifiedAt DESC
    """)
    suspend fun getConflicts(vaultId: String): List<SyncStateEntity>

    /**
     * Get count of notes by status for a vault.
     */
    @Query("SELECT COUNT(*) FROM sync_states WHERE vaultId = :vaultId AND status = :status")
    suspend fun getCountByStatus(vaultId: String, status: SyncStatus): Int

    /**
     * Get count of sync errors for a vault.
     */
    @Query("SELECT COUNT(*) FROM sync_states WHERE vaultId = :vaultId AND status = 'ERROR'")
    suspend fun getErrorCount(vaultId: String): Int

    /**
     * Insert or update a sync state.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syncState: SyncStateEntity)

    /**
     * Insert or update multiple sync states.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(syncStates: List<SyncStateEntity>)

    /**
     * Delete sync state for a note.
     */
    @Query("DELETE FROM sync_states WHERE noteId = :noteId")
    suspend fun delete(noteId: String)

    /**
     * Delete all sync states for a vault.
     */
    @Query("DELETE FROM sync_states WHERE vaultId = :vaultId")
    suspend fun deleteForVault(vaultId: String)

    /**
     * Delete all synced states (cleanup completed syncs).
     */
    @Query("DELETE FROM sync_states WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    /**
     * Reset retry count for failed syncs.
     */
    @Query("UPDATE sync_states SET retryCount = 0 WHERE status = 'ERROR'")
    suspend fun resetRetryCountsForErrors()

    /**
     * Get sync statistics for a vault.
     * Returns a list of status counts.
     */
    @Query("""
        SELECT
            status,
            COUNT(*) as count
        FROM sync_states
        WHERE vaultId = :vaultId
        GROUP BY status
    """)
    suspend fun getSyncStatistics(vaultId: String): List<SyncStatusCount>
}

/**
 * Data class for sync statistics query result.
 */
data class SyncStatusCount(
    val status: SyncStatus,
    val count: Int
)
