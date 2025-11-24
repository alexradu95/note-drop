package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for note operations.
 * Abstracts the data layer from the presentation layer.
 */
interface NoteRepository {
    /**
     * Get all notes as a Flow.
     */
    fun getAllNotes(): Flow<List<Note>>

    /**
     * Get notes by vault ID.
     */
    fun getNotesByVault(vaultId: String): Flow<List<Note>>

    /**
     * Get notes by vault ID as a list (not Flow).
     * Used for sync operations.
     */
    suspend fun getNotesForVault(vaultId: String): List<Note>

    /**
     * Get a single note by ID.
     */
    suspend fun getNoteById(id: String): Note?

    /**
     * Get a single note by ID as Flow.
     */
    fun getNoteByIdFlow(id: String): Flow<Note?>

    /**
     * Search notes by query.
     */
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * Get notes by tag.
     */
    fun getNotesByTag(tag: String): Flow<List<Note>>

    /**
     * Get today's notes.
     */
    fun getTodaysNotes(): Flow<List<Note>>

    /**
     * Get unsynced notes.
     */
    suspend fun getUnsyncedNotes(): List<Note>

    /**
     * Get unsynced notes for a specific vault.
     */
    suspend fun getUnsyncedNotes(vaultId: String): List<Note>

    /**
     * Create a new note.
     */
    suspend fun createNote(note: Note): Result<Note>

    /**
     * Update an existing note.
     */
    suspend fun updateNote(note: Note): Result<Note>

    /**
     * Delete a note.
     */
    suspend fun deleteNote(id: String): Result<Unit>

    /**
     * Delete all notes in a vault.
     */
    suspend fun deleteNotesByVault(vaultId: String): Result<Unit>

    /**
     * Sync a note to its provider.
     */
    suspend fun syncNote(note: Note): Result<Note>
}
