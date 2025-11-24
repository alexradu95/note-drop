package app.notedrop.android.data.provider

import app.notedrop.android.domain.model.ConflictStrategy
import app.notedrop.android.domain.model.FileEvent
import app.notedrop.android.domain.model.FileMetadata
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.NoteMetadata
import app.notedrop.android.domain.model.Vault

/**
 * Interface for note providers (Obsidian, Notion, Local, Capacities, etc.)
 * Each provider implements how to sync notes to external storage.
 *
 * This interface is provider-agnostic and supports:
 * - File-based providers (Obsidian, local folder)
 * - API-based providers (Notion, Capacities)
 * - Custom providers
 */
interface NoteProvider {
    // ========== Basic CRUD Operations ==========

    /**
     * Save a note to the provider's storage.
     * @param note The note to save
     * @param vault The vault configuration
     * @return Success with the file path where the note was saved, or failure result
     */
    suspend fun saveNote(note: Note, vault: Vault): Result<String>

    /**
     * Load a note from the provider's storage by ID.
     * @param noteId The ID of the note to load
     * @param vault The vault configuration
     * @return The loaded note or error
     */
    suspend fun loadNote(noteId: String, vault: Vault): Result<Note>

    /**
     * Load a note from the provider's storage by remote path.
     * Useful for file-based providers where path is the identifier.
     * @param remotePath The path to the note in the provider
     * @param vault The vault configuration
     * @return The loaded note or error
     */
    suspend fun loadNoteByPath(remotePath: String, vault: Vault): Result<Note> {
        // Default implementation delegates to loadNote
        return loadNote(remotePath, vault)
    }

    /**
     * Delete a note from the provider's storage.
     * @param noteId The ID of the note to delete
     * @param vault The vault configuration
     * @return Success or failure result
     */
    suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit>

    // ========== Sync Operations ==========

    /**
     * List all notes in the provider's storage.
     * Returns lightweight metadata without full content.
     * @param vault The vault configuration
     * @return List of note metadata or error
     */
    suspend fun listNotes(vault: Vault): Result<List<NoteMetadata>>

    /**
     * Get metadata for a specific note without loading full content.
     * Useful for checking if a note exists and when it was last modified.
     * @param noteId The ID of the note
     * @param vault The vault configuration
     * @return File metadata or error
     */
    suspend fun getMetadata(noteId: String, vault: Vault): Result<FileMetadata>

    /**
     * Watch for changes in the provider's storage.
     * File-based providers can use FileObserver, API-based can use webhooks.
     * @param vault The vault configuration
     * @param callback Called when a file event occurs
     */
    suspend fun watchChanges(vault: Vault, callback: (FileEvent) -> Unit) {
        // Default implementation: no-op (providers can override)
    }

    /**
     * Stop watching for changes.
     * @param vault The vault configuration
     */
    suspend fun stopWatching(vault: Vault) {
        // Default implementation: no-op
    }

    /**
     * Resolve a sync conflict between local and remote versions.
     * @param localNote The local version of the note
     * @param remoteNote The remote version of the note
     * @param strategy The conflict resolution strategy
     * @param vault The vault configuration
     * @return The resolved note or error
     */
    suspend fun resolveConflict(
        localNote: Note,
        remoteNote: Note,
        strategy: ConflictStrategy,
        vault: Vault
    ): Result<Note> {
        // Default implementation: use strategy to pick winner
        return Result.success(
            when (strategy) {
                ConflictStrategy.LAST_WRITE_WINS ->
                    if (localNote.updatedAt.isAfter(remoteNote.updatedAt)) localNote else remoteNote
                ConflictStrategy.LOCAL_WINS -> localNote
                ConflictStrategy.REMOTE_WINS -> remoteNote
                else -> localNote // Default to local for KEEP_BOTH and MANUAL
            }
        )
    }

    // ========== Provider Information ==========

    /**
     * Check if the provider is available and properly configured.
     * @param vault The vault configuration
     * @return true if provider is ready to use
     */
    suspend fun isAvailable(vault: Vault): Boolean

    /**
     * Get the provider's capabilities.
     * @return Capabilities supported by this provider
     */
    fun getCapabilities(): ProviderCapabilities
}

/**
 * Capabilities that a provider supports.
 * Allows sync engine to adapt behavior based on provider features.
 */
data class ProviderCapabilities(
    // Content types
    val supportsVoiceRecordings: Boolean = false,
    val supportsImages: Boolean = false,
    val supportsAttachments: Boolean = false,

    // Metadata
    val supportsTags: Boolean = true,
    val supportsMetadata: Boolean = true,
    val supportsFrontmatter: Boolean = false,

    // Features
    val supportsSearch: Boolean = false,
    val supportsLinks: Boolean = false,
    val supportsBacklinks: Boolean = false,
    val supportsVersionHistory: Boolean = false,

    // Security
    val supportsEncryption: Boolean = false,

    // Sync
    val supportsBidirectionalSync: Boolean = true,
    val supportsRealTimeSync: Boolean = false,
    val supportsBatchOperations: Boolean = false,
    val requiresInternet: Boolean = false,

    // Storage
    val isFileBased: Boolean = false,
    val isApiBased: Boolean = false,
    val maxNoteSize: Long? = null // bytes, null = unlimited
)
