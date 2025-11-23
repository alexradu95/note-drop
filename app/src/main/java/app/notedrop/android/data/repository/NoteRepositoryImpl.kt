package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NoteRepository.
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesByVault(vaultId: String): Flow<List<Note>> {
        return noteDao.getNotesByVault(vaultId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override fun getNoteByIdFlow(id: String): Flow<Note?> {
        return noteDao.getNoteByIdFlow(id).map { it?.toDomain() }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getNotesByTag(tag: String): Flow<List<Note>> {
        return noteDao.getNotesByTag(tag).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTodaysNotes(): Flow<List<Note>> {
        return getAllNotes().map { notes ->
            val today = LocalDate.now()
            notes.filter { note ->
                val noteDate = Instant.ofEpochMilli(note.createdAt.toEpochMilli())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                noteDate == today
            }
        }
    }

    override suspend fun getUnsyncedNotes(): List<Note> {
        return noteDao.getUnsyncedNotes().map { it.toDomain() }
    }

    override suspend fun createNote(note: Note): Result<Note> {
        return try {
            noteDao.insertNote(note.toEntity())
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        return try {
            val updatedNote = note.copy(updatedAt = Instant.now())
            noteDao.updateNote(updatedNote.toEntity())
            Result.success(updatedNote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            noteDao.deleteNoteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotesByVault(vaultId: String): Result<Unit> {
        return try {
            noteDao.deleteNotesByVault(vaultId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNote(note: Note): Result<Note> {
        // TODO: Implement provider sync
        return try {
            val syncedNote = note.copy(isSynced = true)
            noteDao.updateNote(syncedNote.toEntity())
            Result.success(syncedNote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
