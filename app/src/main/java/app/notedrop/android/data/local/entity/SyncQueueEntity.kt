package app.notedrop.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.notedrop.android.domain.model.SyncQueueItem
import java.time.Instant

/**
 * Room entity for tracking failed sync attempts in a retry queue.
 *
 * Features:
 * - Tracks retry count with max limit (5 retries)
 * - Implements exponential backoff (1min, 5min, 15min, 1hr, 1hr)
 * - Stores failure reason for debugging
 * - Tracks when next retry should be attempted
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val noteId: String,
    val vaultId: String,
    val retryCount: Int = 0,
    val lastAttemptAt: Long, // Epoch milliseconds
    val nextRetryAt: Long, // Epoch milliseconds
    val errorMessage: String?,
    val createdAt: Long // Epoch milliseconds - when first added to queue
)

/**
 * Convert domain model to entity.
 */
fun SyncQueueItem.toEntity(): SyncQueueEntity {
    return SyncQueueEntity(
        noteId = noteId,
        vaultId = vaultId,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt.toEpochMilli(),
        nextRetryAt = nextRetryAt.toEpochMilli(),
        errorMessage = errorMessage,
        createdAt = createdAt.toEpochMilli()
    )
}

/**
 * Convert entity to domain model.
 */
fun SyncQueueEntity.toDomain(): SyncQueueItem {
    return SyncQueueItem(
        noteId = noteId,
        vaultId = vaultId,
        retryCount = retryCount,
        lastAttemptAt = Instant.ofEpochMilli(lastAttemptAt),
        nextRetryAt = Instant.ofEpochMilli(nextRetryAt),
        errorMessage = errorMessage,
        createdAt = Instant.ofEpochMilli(createdAt)
    )
}
