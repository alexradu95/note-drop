package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.SyncQueueItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing the sync retry queue.
 *
 * Handles:
 * - Adding failed syncs to retry queue
 * - Managing retry attempts with exponential backoff
 * - Tracking items that exceed max retries
 * - Removing successfully synced items
 */
interface SyncQueueRepository {

    /**
     * Get all items in the sync queue.
     */
    suspend fun getAllQueueItems(): List<SyncQueueItem>

    /**
     * Observe all items in the sync queue (reactive updates).
     */
    fun observeAllQueueItems(): Flow<List<SyncQueueItem>>

    /**
     * Get items that are ready for retry (nextRetryAt <= current time).
     */
    suspend fun getItemsReadyForRetry(): List<SyncQueueItem>

    /**
     * Get items for a specific vault.
     */
    suspend fun getItemsForVault(vaultId: String): List<SyncQueueItem>

    /**
     * Get items that have exceeded max retry attempts.
     */
    suspend fun getFailedItems(): List<SyncQueueItem>

    /**
     * Get queue item for a specific note.
     */
    suspend fun getQueueItem(noteId: String): SyncQueueItem?

    /**
     * Get total number of items in queue.
     */
    suspend fun getQueueSize(): Int

    /**
     * Get count of items ready for retry.
     */
    suspend fun getReadyForRetryCount(): Int

    /**
     * Add or update a queue item.
     * If the item already exists, it will be updated.
     */
    suspend fun upsert(item: SyncQueueItem)

    /**
     * Add or update multiple queue items.
     */
    suspend fun upsertAll(items: List<SyncQueueItem>)

    /**
     * Remove item from queue (after successful sync).
     */
    suspend fun remove(noteId: String)

    /**
     * Remove all items for a vault.
     */
    suspend fun removeForVault(vaultId: String)

    /**
     * Remove all items that have exceeded max retries.
     */
    suspend fun removeFailedItems()

    /**
     * Clear the entire queue.
     */
    suspend fun clearQueue()

    /**
     * Reset retry count for a specific item (for manual retry).
     */
    suspend fun resetRetryCount(noteId: String)

    /**
     * Reset retry counts for all failed items.
     */
    suspend fun resetAllFailedItems()

    /**
     * Record a failed sync attempt.
     * Creates a new queue item or updates existing one with next retry time.
     */
    suspend fun recordFailedSync(noteId: String, vaultId: String, errorMessage: String?)

    /**
     * Record a successful sync.
     * Removes the item from the queue.
     */
    suspend fun recordSuccessfulSync(noteId: String)
}
