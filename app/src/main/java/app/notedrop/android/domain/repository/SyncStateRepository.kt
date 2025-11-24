package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing synchronization state.
 * Tracks the sync status of notes across different vaults and providers.
 */
interface SyncStateRepository {

    /**
     * Get sync state for a specific note.
     */
    suspend fun getSyncState(noteId: String): SyncState?

    /**
     * Observe sync state for a specific note (reactive updates).
     */
    fun observeSyncState(noteId: String): Flow<SyncState?>

    /**
     * Get all sync states for a vault.
     */
    fun getSyncStatesForVault(vaultId: String): Flow<List<SyncState>>

    /**
     * Get all sync states with a specific status.
     */
    suspend fun getByStatus(status: SyncStatus): List<SyncState>

    /**
     * Get all sync states with a specific status for a vault.
     */
    suspend fun getByStatusForVault(vaultId: String, status: SyncStatus): List<SyncState>

    /**
     * Get notes that need to be uploaded.
     */
    suspend fun getPendingUploads(vaultId: String, maxRetries: Int = 3): List<SyncState>

    /**
     * Get notes that need to be downloaded.
     */
    suspend fun getPendingDownloads(vaultId: String): List<SyncState>

    /**
     * Get notes with conflicts.
     */
    suspend fun getConflicts(vaultId: String): List<SyncState>

    /**
     * Get count of notes by status for a vault.
     */
    suspend fun getCountByStatus(vaultId: String, status: SyncStatus): Int

    /**
     * Get count of sync errors for a vault.
     */
    suspend fun getErrorCount(vaultId: String): Int

    /**
     * Save or update sync state.
     */
    suspend fun upsert(syncState: SyncState)

    /**
     * Save or update multiple sync states.
     */
    suspend fun upsertAll(syncStates: List<SyncState>)

    /**
     * Delete sync state for a note.
     */
    suspend fun delete(noteId: String)

    /**
     * Delete all sync states for a vault.
     */
    suspend fun deleteForVault(vaultId: String)

    /**
     * Delete all synced states (cleanup completed syncs).
     */
    suspend fun deleteSynced()

    /**
     * Reset retry counts for failed syncs.
     */
    suspend fun resetRetryCountsForErrors()

    /**
     * Get sync statistics for a vault.
     */
    suspend fun getSyncStatistics(vaultId: String): Map<SyncStatus, Int>
}
