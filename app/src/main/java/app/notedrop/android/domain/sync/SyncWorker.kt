package app.notedrop.android.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.notedrop.android.domain.model.SyncStatus
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.SyncQueueRepository
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.domain.repository.VaultRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker for periodic note synchronization.
 *
 * Features:
 * - Runs every 15 minutes when connected to WiFi
 * - Syncs notes that failed previously (from sync queue)
 * - Syncs notes marked as unsynced (isSynced = false)
 * - Implements exponential backoff for failed syncs
 * - Handles network connectivity checks
 *
 * Scheduled by WorkManager in Application class.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncCoordinator: SyncCoordinator,
    private val syncQueueRepository: SyncQueueRepository,
    private val syncStateRepository: SyncStateRepository,
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker starting...")

        return try {
            // Get all vaults
            val vaults = vaultRepository.getAllVaults().first()

            if (vaults.isEmpty()) {
                Log.d(TAG, "No vaults configured, skipping sync")
                return Result.success()
            }

            var totalSynced = 0
            var totalFailed = 0

            vaults.forEach { vault ->
                Log.d(TAG, "Processing vault: ${vault.name} (${vault.id})")

                // 1. Process retry queue for this vault
                val readyForRetry = syncQueueRepository.getItemsReadyForRetry()
                    .filter { it.vaultId == vault.id }

                Log.d(TAG, "Found ${readyForRetry.size} items ready for retry")

                readyForRetry.forEach { queueItem ->
                    if (queueItem.hasExceededMaxRetries()) {
                        Log.w(TAG, "Note ${queueItem.noteId} exceeded max retries, skipping")
                        return@forEach
                    }

                    Log.d(TAG, "Retrying sync for note ${queueItem.noteId} (attempt ${queueItem.retryCount + 1})")

                    // Attempt sync
                    syncCoordinator.syncNote(queueItem.noteId)
                        .onSuccess {
                            Log.d(TAG, "Successfully synced note ${queueItem.noteId}")
                            syncQueueRepository.recordSuccessfulSync(queueItem.noteId)
                            totalSynced++
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to sync note ${queueItem.noteId}: ${error.message}", error)
                            syncQueueRepository.recordFailedSync(
                                noteId = queueItem.noteId,
                                vaultId = queueItem.vaultId,
                                errorMessage = error.message
                            )
                            totalFailed++
                        }
                }

                // 2. Sync unsynced notes (isSynced = false)
                val unsyncedNotes = noteRepository.getUnsyncedNotes(vault.id)
                Log.d(TAG, "Found ${unsyncedNotes.size} unsynced notes")

                unsyncedNotes.forEach { note ->
                    // Check if already in retry queue
                    val inQueue = syncQueueRepository.getQueueItem(note.id) != null
                    if (inQueue) {
                        Log.d(TAG, "Note ${note.id} already in retry queue, skipping")
                        return@forEach
                    }

                    Log.d(TAG, "Syncing unsynced note ${note.id}")

                    syncCoordinator.syncNote(note.id)
                        .onSuccess {
                            Log.d(TAG, "Successfully synced note ${note.id}")
                            totalSynced++
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to sync note ${note.id}: ${error.message}", error)
                            syncQueueRepository.recordFailedSync(
                                noteId = note.id,
                                vaultId = vault.id,
                                errorMessage = error.message
                            )
                            totalFailed++
                        }
                }

                // 3. Sync notes with pending upload status
                val pendingUploads = syncStateRepository.getPendingUploads(vault.id, maxRetries = 5)
                Log.d(TAG, "Found ${pendingUploads.size} notes pending upload")

                pendingUploads.forEach { syncState ->
                    // Check if already in retry queue
                    val inQueue = syncQueueRepository.getQueueItem(syncState.noteId) != null
                    if (inQueue) {
                        return@forEach
                    }

                    syncCoordinator.syncNote(syncState.noteId)
                        .onSuccess {
                            Log.d(TAG, "Successfully synced note ${syncState.noteId}")
                            totalSynced++
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to sync note ${syncState.noteId}: ${error.message}", error)
                            syncQueueRepository.recordFailedSync(
                                noteId = syncState.noteId,
                                vaultId = vault.id,
                                errorMessage = error.message
                            )
                            totalFailed++
                        }
                }
            }

            Log.d(TAG, "SyncWorker completed: $totalSynced synced, $totalFailed failed")

            // Return success even if some syncs failed (they're in the queue for retry)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker error: ${e.message}", e)
            // Retry on failure
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
        const val PERIODIC_SYNC_INTERVAL_MINUTES = 15L
    }
}
