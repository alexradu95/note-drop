package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Note
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for note operations.
 * Abstracts the data layer from the presentation layer.
 *
 * All mutating operations return Result<T, AppError> for type-safe error handling.
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
    suspend fun getNotesForVault(vaultId: String): Result<List<Note>, AppError>

    /**
     * Get a single note by ID.
     */
    suspend fun getNoteById(id: String): Result<Note, AppError>

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
    suspend fun getUnsyncedNotes(): Result<List<Note>, AppError>

    /**
     * Get unsynced notes for a specific vault.
     */
    suspend fun getUnsyncedNotes(vaultId: String): Result<List<Note>, AppError>

    /**
     * Create a new note.
     */
    suspend fun createNote(note: Note): Result<Note, AppError>

    /**
     * Update an existing note.
     */
    suspend fun updateNote(note: Note): Result<Note, AppError>

    /**
     * Delete a note.
     */
    suspend fun deleteNote(id: String): Result<Unit, AppError>

    /**
     * Delete all notes in a vault.
     */
    suspend fun deleteNotesByVault(vaultId: String): Result<Unit, AppError>

    /**
     * Sync a note to its provider.
     */
    suspend fun syncNote(note: Note): Result<Note, AppError>
}
