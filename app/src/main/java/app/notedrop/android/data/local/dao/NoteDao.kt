package app.notedrop.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.notedrop.android.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notes.
 */
@Dao
interface NoteDao {
    /**
     * Get all notes as a Flow (live updates).
     */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /**
     * Get notes by vault ID.
     */
    @Query("SELECT * FROM notes WHERE vaultId = :vaultId ORDER BY createdAt DESC")
    fun getNotesByVault(vaultId: String): Flow<List<NoteEntity>>

    /**
     * Get a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    /**
     * Get a single note by ID as Flow.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteByIdFlow(id: String): Flow<NoteEntity?>

    /**
     * Search notes by content or title.
     */
    @Query("""
        SELECT * FROM notes
        WHERE content LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    /**
     * Get notes by tag.
     */
    @Query("SELECT * FROM notes WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getNotesByTag(tag: String): Flow<List<NoteEntity>>

    /**
     * Get unsynced notes.
     */
    @Query("SELECT * FROM notes WHERE isSynced = 0 ORDER BY createdAt DESC")
    suspend fun getUnsyncedNotes(): List<NoteEntity>

    /**
     * Get notes count by vault.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE vaultId = :vaultId")
    suspend fun getNotesCountByVault(vaultId: String): Int

    /**
     * Insert a new note.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    /**
     * Insert multiple notes.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    /**
     * Update an existing note.
     */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * Delete a note.
     */
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    /**
     * Delete note by ID.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    /**
     * Delete all notes in a vault.
     */
    @Query("DELETE FROM notes WHERE vaultId = :vaultId")
    suspend fun deleteNotesByVault(vaultId: String)

    /**
     * Delete all notes.
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
