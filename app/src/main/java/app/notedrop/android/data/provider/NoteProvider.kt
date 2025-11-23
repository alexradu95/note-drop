package app.notedrop.android.data.provider

import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.Vault

/**
 * Interface for note providers (Obsidian, Notion, etc.)
 * Each provider implements how to sync notes to external storage.
 */
interface NoteProvider {
    /**
     * Save a note to the provider's storage.
     */
    suspend fun saveNote(note: Note, vault: Vault): Result<Unit>

    /**
     * Load a note from the provider's storage.
     */
    suspend fun loadNote(noteId: String, vault: Vault): Result<Note>

    /**
     * Delete a note from the provider's storage.
     */
    suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit>

    /**
     * Check if the provider is available and configured.
     */
    suspend fun isAvailable(vault: Vault): Boolean

    /**
     * Get the provider's capabilities.
     */
    fun getCapabilities(): ProviderCapabilities
}

/**
 * Capabilities that a provider supports.
 */
data class ProviderCapabilities(
    val supportsVoiceRecordings: Boolean = false,
    val supportsImages: Boolean = false,
    val supportsTags: Boolean = true,
    val supportsMetadata: Boolean = true,
    val supportsEncryption: Boolean = false,
    val requiresInternet: Boolean = false
)
