package app.notedrop.android.domain.sync

import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.domain.model.ConflictStrategy
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.SyncMode
import app.notedrop.android.domain.model.SyncResult
import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.model.SyncStatus
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.domain.repository.VaultRepository
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncCoordinator.
 * Provider-agnostic sync orchestration.
 */
@Singleton
class SyncCoordinatorImpl @Inject constructor(
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository,
    private val syncStateRepository: SyncStateRepository,
    private val conflictResolver: ConflictResolver,
    private val providerFactory: ProviderFactory
) : SyncCoordinator {

    private val activeSyncs = mutableMapOf<String, Job>()

    override suspend fun syncVault(vaultId: String): Result<SyncResult> = coroutineScope {
        try {
            // Check if sync is already running
            if (activeSyncs.containsKey(vaultId)) {
                return@coroutineScope Result.failure(Exception("Sync already in progress for vault $vaultId"))
            }

            val vault = vaultRepository.getVaultById(vaultId).getOrElse { error ->
                return@coroutineScope Result.failure(Exception("Vault not found: $vaultId - $error"))
            }

            val config = vault.providerConfig
            val syncMode = getSyncMode(config)

            if (syncMode == SyncMode.DISABLED) {
                return@coroutineScope Result.success(
                    SyncResult(0, 0, 0, 0, 0, 0)
                )
            }

            var uploaded = 0
            var downloaded = 0
            var conflicts = 0
            var errors = 0
            var skipped = 0

            // Push local changes
            if (syncMode == SyncMode.PUSH_ONLY || syncMode == SyncMode.BIDIRECTIONAL) {
                pushChanges(vaultId).onSuccess { uploaded = it }.onFailure { errors++ }
            }

            // Pull remote changes
            if (syncMode == SyncMode.PULL_ONLY || syncMode == SyncMode.BIDIRECTIONAL) {
                pullChanges(vaultId).onSuccess { downloaded = it }.onFailure { errors++ }
            }

            // Resolve conflicts if bidirectional
            if (syncMode == SyncMode.BIDIRECTIONAL) {
                val conflictStates = syncStateRepository.getConflicts(vaultId)
                conflicts = conflictStates.size

                // Attempt automatic resolution
                resolveConflicts(vaultId).onSuccess { resolved ->
                    conflicts -= resolved
                }
            }

            val result = SyncResult(
                totalNotes = uploaded + downloaded + conflicts + errors + skipped,
                uploaded = uploaded,
                downloaded = downloaded,
                conflicts = conflicts,
                errors = errors,
                skipped = skipped
            )

            // Update vault last synced time
            if (result.isSuccess) {
                vaultRepository.updateLastSynced(vaultId)
            }

            Result.success(result)
        } catch (e: CancellationException) {
            Result.failure(Exception("Sync cancelled"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            activeSyncs.remove(vaultId)
        }
    }

    override suspend fun syncNote(noteId: String): Result<Unit> {
        return try {
            val note = noteRepository.getNoteById(noteId).getOrElse { error ->
                return Result.failure(Exception("Note not found: $noteId - $error"))
            }

            val vault = vaultRepository.getVaultById(note.vaultId).getOrElse { error ->
                return Result.failure(Exception("Vault not found: ${note.vaultId} - $error"))
            }

            val provider = providerFactory.getProvider(vault.providerType)

            // Check if provider is available
            if (!provider.isAvailable(vault)) {
                return Result.failure(Exception("Provider not available"))
            }

            val syncState = syncStateRepository.getSyncState(noteId)

            if (syncState == null || syncState.needsPush()) {
                // Push note to provider
                provider.saveNote(note, vault).onSuccess {
                    // Update sync state
                    syncStateRepository.upsert(
                        SyncState(
                            noteId = noteId,
                            vaultId = vault.id,
                            status = SyncStatus.SYNCED,
                            localModifiedAt = note.updatedAt,
                            remoteModifiedAt = Instant.now(),
                            lastSyncedAt = Instant.now(),
                            remotePath = null // Provider can update this
                        )
                    )
                }.onFailure { error ->
                    // Update sync state with error
                    syncStateRepository.upsert(
                        SyncState(
                            noteId = noteId,
                            vaultId = vault.id,
                            status = SyncStatus.ERROR,
                            localModifiedAt = note.updatedAt,
                            remoteModifiedAt = syncState?.remoteModifiedAt,
                            lastSyncedAt = syncState?.lastSyncedAt,
                            remotePath = syncState?.remotePath,
                            retryCount = (syncState?.retryCount ?: 0) + 1,
                            errorMessage = error.message
                        )
                    )
                    return Result.failure(error)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pushChanges(vaultId: String): Result<Int> = coroutineScope {
        try {
            if (!isActive) throw CancellationException()

            val vault = vaultRepository.getVaultById(vaultId).getOrElse { error ->
                return@coroutineScope Result.failure(Exception("Vault not found - $error"))
            }

            val provider = providerFactory.getProvider(vault.providerType)

            if (!provider.isAvailable(vault)) {
                return@coroutineScope Result.failure(Exception("Provider not available"))
            }

            // Get notes pending upload
            val pendingUploads = syncStateRepository.getPendingUploads(vaultId, maxRetries = 3)

            var uploadedCount = 0

            pendingUploads.forEach { syncState ->
                if (!isActive) throw CancellationException()

                val note = noteRepository.getNoteById(syncState.noteId).getOrElse {
                    // Note was deleted locally or error occurred
                    syncStateRepository.delete(syncState.noteId)
                    return@forEach
                }

                // Upload note
                provider.saveNote(note, vault).onSuccess {
                    // Update sync state
                    syncStateRepository.upsert(
                        syncState.copy(
                            status = SyncStatus.SYNCED,
                            remoteModifiedAt = Instant.now(),
                            lastSyncedAt = Instant.now(),
                            retryCount = 0,
                            errorMessage = null
                        )
                    )
                    uploadedCount++

                    // Update note as synced
                    noteRepository.updateNote(note.copy(isSynced = true)).getOrElse { /* ignore update error */ }
                }.onFailure { error ->
                    // Update sync state with error
                    syncStateRepository.upsert(
                        syncState.copy(
                            status = SyncStatus.ERROR,
                            retryCount = syncState.retryCount + 1,
                            errorMessage = error.message
                        )
                    )
                }
            }

            Result.success(uploadedCount)
        } catch (e: CancellationException) {
            Result.failure(Exception("Push cancelled"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pullChanges(vaultId: String): Result<Int> = coroutineScope {
        try {
            if (!isActive) throw CancellationException()

            val vault = vaultRepository.getVaultById(vaultId).getOrElse { error ->
                return@coroutineScope Result.failure(Exception("Vault not found - $error"))
            }

            val provider = providerFactory.getProvider(vault.providerType)

            if (!provider.isAvailable(vault)) {
                return@coroutineScope Result.failure(Exception("Provider not available"))
            }

            // List all notes in the provider
            val remoteNotes = provider.listNotes(vault).getOrNull()
                ?: return@coroutineScope Result.failure(Exception("Failed to list remote notes"))

            var downloadedCount = 0

            remoteNotes.forEach { metadata ->
                if (!isActive) throw CancellationException()

                val syncState = syncStateRepository.getSyncState(metadata.id)
                val localNote = noteRepository.getNoteById(metadata.id).getOrElse { null }

                // Check if we need to download this note
                val shouldDownload = syncState == null ||
                    syncState.remoteModifiedAt == null ||
                    metadata.modifiedAt.isAfter(syncState.remoteModifiedAt)

                if (shouldDownload) {
                    // Download note from provider
                    provider.loadNote(metadata.id, vault).onSuccess { remoteNote ->
                        if (localNote == null) {
                            // New note from remote
                            noteRepository.createNote(remoteNote.copy(isSynced = true)).getOrElse { return@onSuccess }
                            downloadedCount++

                            // Create sync state
                            syncStateRepository.upsert(
                                SyncState(
                                    noteId = remoteNote.id,
                                    vaultId = vaultId,
                                    status = SyncStatus.SYNCED,
                                    localModifiedAt = remoteNote.updatedAt,
                                    remoteModifiedAt = metadata.modifiedAt,
                                    lastSyncedAt = Instant.now(),
                                    remotePath = metadata.path
                                )
                            )
                        } else {
                            // Check for conflict
                            if (syncState != null && syncState.hasConflict()) {
                                // Mark as conflict
                                syncStateRepository.upsert(
                                    syncState.copy(
                                        status = SyncStatus.CONFLICT,
                                        remoteModifiedAt = metadata.modifiedAt
                                    )
                                )
                            } else {
                                // Update local note with remote version
                                noteRepository.updateNote(remoteNote.copy(isSynced = true)).getOrElse { return@onSuccess }
                                downloadedCount++

                                // Update sync state
                                syncStateRepository.upsert(
                                    SyncState(
                                        noteId = remoteNote.id,
                                        vaultId = vaultId,
                                        status = SyncStatus.SYNCED,
                                        localModifiedAt = remoteNote.updatedAt,
                                        remoteModifiedAt = metadata.modifiedAt,
                                        lastSyncedAt = Instant.now(),
                                        remotePath = metadata.path
                                    )
                                )
                            }
                        }
                    }.onFailure { error ->
                        // Log error but continue with other notes
                        syncStateRepository.upsert(
                            SyncState(
                                noteId = metadata.id,
                                vaultId = vaultId,
                                status = SyncStatus.ERROR,
                                localModifiedAt = localNote?.updatedAt ?: Instant.now(),
                                remoteModifiedAt = metadata.modifiedAt,
                                lastSyncedAt = syncState?.lastSyncedAt,
                                remotePath = metadata.path,
                                retryCount = (syncState?.retryCount ?: 0) + 1,
                                errorMessage = error.message
                            )
                        )
                    }
                }
            }

            Result.success(downloadedCount)
        } catch (e: CancellationException) {
            Result.failure(Exception("Pull cancelled"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveConflicts(vaultId: String): Result<Int> {
        return try {
            val vault = vaultRepository.getVaultById(vaultId).getOrElse { error ->
                return Result.failure(Exception("Vault not found - $error"))
            }

            val provider = providerFactory.getProvider(vault.providerType)
            val strategy = getConflictStrategy(vault.providerConfig)

            val conflicts = syncStateRepository.getConflicts(vaultId)
            var resolvedCount = 0

            conflicts.forEach { syncState ->
                val localNote = noteRepository.getNoteById(syncState.noteId).getOrElse {
                    syncStateRepository.delete(syncState.noteId)
                    return@forEach
                }

                // Load remote version
                provider.loadNote(syncState.noteId, vault).onSuccess { remoteNote ->
                    // Resolve conflict
                    val resolution = conflictResolver.resolve(localNote, remoteNote, strategy)

                    when (resolution) {
                        is ConflictResolution.UseLocal -> {
                            // Keep local, update remote
                            provider.saveNote(localNote, vault)
                            syncStateRepository.upsert(
                                syncState.copy(
                                    status = SyncStatus.SYNCED,
                                    remoteModifiedAt = Instant.now(),
                                    lastSyncedAt = Instant.now()
                                )
                            )
                            resolvedCount++
                        }

                        is ConflictResolution.UseRemote -> {
                            // Keep remote, update local
                            noteRepository.updateNote(remoteNote).getOrElse { return@onSuccess }
                            syncStateRepository.upsert(
                                syncState.copy(
                                    status = SyncStatus.SYNCED,
                                    localModifiedAt = remoteNote.updatedAt,
                                    lastSyncedAt = Instant.now()
                                )
                            )
                            resolvedCount++
                        }

                        is ConflictResolution.KeepBoth -> {
                            // Keep both - save conflict copy to remote
                            provider.saveNote(resolution.local, vault)
                            provider.saveNote(resolution.remote, vault)
                            syncStateRepository.upsert(
                                syncState.copy(
                                    status = SyncStatus.SYNCED,
                                    lastSyncedAt = Instant.now()
                                )
                            )
                            resolvedCount++
                        }

                        is ConflictResolution.Merged -> {
                            // Merged successfully
                            noteRepository.updateNote(resolution.note).getOrElse { return@onSuccess }
                            provider.saveNote(resolution.note, vault)
                            syncStateRepository.upsert(
                                syncState.copy(
                                    status = SyncStatus.SYNCED,
                                    localModifiedAt = resolution.note.updatedAt,
                                    remoteModifiedAt = Instant.now(),
                                    lastSyncedAt = Instant.now()
                                )
                            )
                            resolvedCount++
                        }

                        is ConflictResolution.RequiresManual -> {
                            // Leave in conflict state for manual resolution
                            // Update error message
                            syncStateRepository.upsert(
                                syncState.copy(
                                    errorMessage = resolution.reason
                                )
                            )
                        }
                    }
                }
            }

            Result.success(resolvedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forceResync(vaultId: String): Result<SyncResult> {
        // Delete all sync states for the vault
        syncStateRepository.deleteForVault(vaultId)

        // Get all notes for the vault
        val notes = noteRepository.getNotesForVault(vaultId).getOrElse { error ->
            return Result.failure(Exception("Failed to get notes for vault: $error"))
        }

        // Create new sync states for all notes
        val syncStates = notes.map { note ->
            SyncState(
                noteId = note.id,
                vaultId = vaultId,
                status = if (note.isSynced) SyncStatus.SYNCED else SyncStatus.PENDING_UPLOAD,
                localModifiedAt = note.updatedAt,
                remoteModifiedAt = null,
                lastSyncedAt = null
            )
        }
        syncStateRepository.upsertAll(syncStates)

        // Perform full sync
        return syncVault(vaultId)
    }

    override suspend fun getSyncProgress(vaultId: String): Int {
        val total = syncStateRepository.getCountByStatus(vaultId, SyncStatus.PENDING_UPLOAD) +
            syncStateRepository.getCountByStatus(vaultId, SyncStatus.PENDING_DOWNLOAD) +
            syncStateRepository.getCountByStatus(vaultId, SyncStatus.SYNCED)

        if (total == 0) return 100

        val synced = syncStateRepository.getCountByStatus(vaultId, SyncStatus.SYNCED)
        return (synced * 100) / total
    }

    override suspend fun cancelSync(vaultId: String) {
        activeSyncs[vaultId]?.cancel()
        activeSyncs.remove(vaultId)
    }

    /**
     * Get sync mode from provider config.
     */
    private fun getSyncMode(config: ProviderConfig): SyncMode {
        return when (config) {
            is ProviderConfig.ObsidianConfig -> config.syncMode
            is ProviderConfig.LocalConfig -> SyncMode.DISABLED
        }
    }

    /**
     * Get conflict strategy from provider config.
     */
    private fun getConflictStrategy(config: ProviderConfig): ConflictStrategy {
        return when (config) {
            is ProviderConfig.ObsidianConfig -> config.conflictStrategy
            is ProviderConfig.LocalConfig -> ConflictStrategy.LAST_WRITE_WINS
        }
    }
}
