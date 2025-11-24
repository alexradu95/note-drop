package app.notedrop.android.domain.model

import java.time.Instant
import kotlin.math.min
import kotlin.math.pow

/**
 * Domain model representing an item in the sync retry queue.
 *
 * Implements exponential backoff strategy:
 * - Retry 1: 1 minute
 * - Retry 2: 5 minutes
 * - Retry 3: 15 minutes
 * - Retry 4: 1 hour
 * - Retry 5: 1 hour (max)
 *
 * After 5 failed retries, the item remains in ERROR state until manual intervention.
 */
data class SyncQueueItem(
    val noteId: String,
    val vaultId: String,
    val retryCount: Int = 0,
    val lastAttemptAt: Instant,
    val nextRetryAt: Instant,
    val errorMessage: String?,
    val createdAt: Instant
) {
    /**
     * Check if this item has exceeded max retry attempts.
     */
    fun hasExceededMaxRetries(): Boolean = retryCount >= MAX_RETRIES

    /**
     * Check if this item is ready for retry (current time >= nextRetryAt).
     */
    fun isReadyForRetry(): Boolean = Instant.now() >= nextRetryAt

    /**
     * Create a new queue item for the next retry attempt.
     */
    fun nextRetry(newErrorMessage: String? = null): SyncQueueItem {
        val newRetryCount = retryCount + 1
        val now = Instant.now()
        val backoffMinutes = calculateBackoff(newRetryCount)

        return copy(
            retryCount = newRetryCount,
            lastAttemptAt = now,
            nextRetryAt = now.plusSeconds(backoffMinutes * 60),
            errorMessage = newErrorMessage ?: errorMessage
        )
    }

    companion object {
        const val MAX_RETRIES = 5

        /**
         * Calculate backoff delay in minutes based on retry count.
         * Uses exponential backoff with a cap.
         */
        fun calculateBackoff(retryCount: Int): Long {
            return when (retryCount) {
                1 -> 1L      // 1 minute
                2 -> 5L      // 5 minutes
                3 -> 15L     // 15 minutes
                4 -> 60L     // 1 hour
                else -> 60L  // 1 hour (capped)
            }
        }

        /**
         * Create a new queue item for a failed sync.
         */
        fun create(
            noteId: String,
            vaultId: String,
            errorMessage: String?
        ): SyncQueueItem {
            val now = Instant.now()
            return SyncQueueItem(
                noteId = noteId,
                vaultId = vaultId,
                retryCount = 0,
                lastAttemptAt = now,
                nextRetryAt = now.plusSeconds(60), // First retry after 1 minute
                errorMessage = errorMessage,
                createdAt = now
            )
        }
    }
}
