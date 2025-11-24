package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.SyncQueueDao
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.domain.model.SyncQueueItem
import app.notedrop.android.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncQueueRepository using Room database.
 */
@Singleton
class SyncQueueRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) : SyncQueueRepository {

    override suspend fun getAllQueueItems(): List<SyncQueueItem> {
        return syncQueueDao.getAllQueueItems().map { it.toDomain() }
    }

    override fun observeAllQueueItems(): Flow<List<SyncQueueItem>> {
        return syncQueueDao.observeAllQueueItems().map { items ->
            items.map { it.toDomain() }
        }
    }

    override suspend fun getItemsReadyForRetry(): List<SyncQueueItem> {
        val currentTime = Instant.now().toEpochMilli()
        return syncQueueDao.getItemsReadyForRetry(currentTime).map { it.toDomain() }
    }

    override suspend fun getItemsForVault(vaultId: String): List<SyncQueueItem> {
        return syncQueueDao.getItemsForVault(vaultId).map { it.toDomain() }
    }

    override suspend fun getFailedItems(): List<SyncQueueItem> {
        return syncQueueDao.getFailedItems().map { it.toDomain() }
    }

    override suspend fun getQueueItem(noteId: String): SyncQueueItem? {
        return syncQueueDao.getQueueItem(noteId)?.toDomain()
    }

    override suspend fun getQueueSize(): Int {
        return syncQueueDao.getQueueSize()
    }

    override suspend fun getReadyForRetryCount(): Int {
        val currentTime = Instant.now().toEpochMilli()
        return syncQueueDao.getReadyForRetryCount(currentTime)
    }

    override suspend fun upsert(item: SyncQueueItem) {
        syncQueueDao.upsert(item.toEntity())
    }

    override suspend fun upsertAll(items: List<SyncQueueItem>) {
        syncQueueDao.upsertAll(items.map { it.toEntity() })
    }

    override suspend fun remove(noteId: String) {
        syncQueueDao.delete(noteId)
    }

    override suspend fun removeForVault(vaultId: String) {
        syncQueueDao.deleteForVault(vaultId)
    }

    override suspend fun removeFailedItems() {
        syncQueueDao.deleteFailedItems()
    }

    override suspend fun clearQueue() {
        syncQueueDao.clearQueue()
    }

    override suspend fun resetRetryCount(noteId: String) {
        val nextRetryAt = Instant.now().plusSeconds(60).toEpochMilli()
        syncQueueDao.resetRetryCount(noteId, nextRetryAt)
    }

    override suspend fun resetAllFailedItems() {
        val nextRetryAt = Instant.now().plusSeconds(60).toEpochMilli()
        syncQueueDao.resetAllFailedItems(nextRetryAt)
    }

    override suspend fun recordFailedSync(noteId: String, vaultId: String, errorMessage: String?) {
        val existing = syncQueueDao.getQueueItem(noteId)

        val queueItem = if (existing != null) {
            // Update existing item with next retry
            existing.toDomain().nextRetry(errorMessage)
        } else {
            // Create new queue item
            SyncQueueItem.create(noteId, vaultId, errorMessage)
        }

        syncQueueDao.upsert(queueItem.toEntity())
    }

    override suspend fun recordSuccessfulSync(noteId: String) {
        syncQueueDao.delete(noteId)
    }
}
