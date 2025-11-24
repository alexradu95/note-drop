package app.notedrop.android.domain.sync

import app.notedrop.android.domain.model.SyncResult

/**
 * Coordinates synchronization between local storage and external providers.
 * This is the main sync engine - completely provider-agnostic.
 *
 * The coordinator handles:
 * - Push sync (local → remote)
 * - Pull sync (remote → local)
 * - Conflict detection and resolution
 * - Retry logic for failed syncs
 * - Sync state management
 */
interface SyncCoordinator {

    /**
     * Perform a full sync for a vault (both push and pull).
     *
     * @param vaultId The vault to sync
     * @return Sync result with counts of uploaded, downloaded, conflicts, and errors
     */
    suspend fun syncVault(vaultId: String): Result<SyncResult>

    /**
     * Sync a specific note.
     *
     * @param noteId The note to sync
     * @return Success or failure
     */
    suspend fun syncNote(noteId: String): Result<Unit>

    /**
     * Push pending local changes to the remote provider.
     *
     * @param vaultId The vault to push changes for
     * @return Number of notes uploaded
     */
    suspend fun pushChanges(vaultId: String): Result<Int>

    /**
     * Pull remote changes from the provider to local storage.
     *
     * @param vaultId The vault to pull changes for
     * @return Number of notes downloaded
     */
    suspend fun pullChanges(vaultId: String): Result<Int>

    /**
     * Resolve all conflicts for a vault.
     *
     * @param vaultId The vault to resolve conflicts for
     * @return Number of conflicts resolved
     */
    suspend fun resolveConflicts(vaultId: String): Result<Int>

    /**
     * Force a full resync of a vault (reset all sync states and re-sync).
     *
     * @param vaultId The vault to resync
     * @return Sync result
     */
    suspend fun forceResync(vaultId: String): Result<SyncResult>

    /**
     * Get sync progress for a vault (percentage complete).
     *
     * @param vaultId The vault to check
     * @return Progress percentage (0-100)
     */
    suspend fun getSyncProgress(vaultId: String): Int

    /**
     * Cancel ongoing sync for a vault.
     *
     * @param vaultId The vault to cancel sync for
     */
    suspend fun cancelSync(vaultId: String)
}
