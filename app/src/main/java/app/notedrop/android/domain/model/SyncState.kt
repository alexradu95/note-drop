package app.notedrop.android.domain.model

import java.time.Instant

/**
 * Domain model representing the synchronization state of a note.
 * This is provider-agnostic and works with any storage backend.
 *
 * @property noteId ID of the note being synced
 * @property vaultId ID of the vault this note belongs to
 * @property status Current sync status
 * @property localModifiedAt Last modification time on local device
 * @property remoteModifiedAt Last modification time on remote provider (null if not yet synced)
 * @property lastSyncedAt Timestamp of last successful sync
 * @property remotePath Path/identifier in the remote provider (e.g., "daily/2024-01-15.md" for Obsidian)
 * @property retryCount Number of failed sync attempts
 * @property errorMessage Last error message if sync failed
 */
data class SyncState(
    val noteId: String,
    val vaultId: String,
    val status: SyncStatus,
    val localModifiedAt: Instant,
    val remoteModifiedAt: Instant? = null,
    val lastSyncedAt: Instant? = null,
    val remotePath: String? = null,
    val retryCount: Int = 0,
    val errorMessage: String? = null
) {
    /**
     * Check if this note has a conflict (both local and remote changed after last sync).
     */
    fun hasConflict(): Boolean {
        val lastSync = lastSyncedAt ?: return false
        val remoteModified = remoteModifiedAt ?: return false
        return localModifiedAt.isAfter(lastSync) && remoteModified.isAfter(lastSync)
    }

    /**
     * Check if local changes need to be pushed.
     */
    fun needsPush(): Boolean {
        return lastSyncedAt == null || localModifiedAt.isAfter(lastSyncedAt)
    }

    /**
     * Check if remote changes need to be pulled.
     */
    fun needsPull(): Boolean {
        val remoteModified = remoteModifiedAt ?: return false
        val lastSync = lastSyncedAt ?: return true
        return remoteModified.isAfter(lastSync)
    }
}

/**
 * Synchronization status for a note.
 * Provider-agnostic status tracking.
 */
enum class SyncStatus {
    /**
     * Local note has changes that need to be uploaded to remote provider.
     */
    PENDING_UPLOAD,

    /**
     * Remote provider has changes that need to be downloaded locally.
     */
    PENDING_DOWNLOAD,

    /**
     * Local and remote are in sync.
     */
    SYNCED,

    /**
     * Both local and remote have changes - conflict needs resolution.
     */
    CONFLICT,

    /**
     * Sync failed due to an error.
     */
    ERROR,

    /**
     * Initial state - note has never been synced.
     */
    NEVER_SYNCED
}

/**
 * Result of a sync operation.
 */
data class SyncResult(
    val totalNotes: Int,
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val errors: Int,
    val skipped: Int
) {
    val isSuccess: Boolean
        get() = errors == 0 && conflicts == 0

    val hasConflicts: Boolean
        get() = conflicts > 0
}

/**
 * Strategy for resolving sync conflicts.
 * Allows different providers or vaults to use different strategies.
 */
enum class ConflictStrategy {
    /**
     * The version with the newer timestamp wins.
     */
    LAST_WRITE_WINS,

    /**
     * Keep both versions - remote becomes "Note (conflict).md".
     */
    KEEP_BOTH,

    /**
     * Local always wins - remote changes are discarded.
     */
    LOCAL_WINS,

    /**
     * Remote always wins - local changes are discarded.
     */
    REMOTE_WINS,

    /**
     * Require manual user resolution.
     */
    MANUAL
}

/**
 * Sync mode determines the direction of synchronization.
 */
enum class SyncMode {
    /**
     * Only upload from local to remote (one-way backup).
     */
    PUSH_ONLY,

    /**
     * Only download from remote to local (one-way import).
     */
    PULL_ONLY,

    /**
     * Synchronize in both directions (full sync).
     */
    BIDIRECTIONAL,

    /**
     * Sync is disabled for this vault.
     */
    DISABLED
}
