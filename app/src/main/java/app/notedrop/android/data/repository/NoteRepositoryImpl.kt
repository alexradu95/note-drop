package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.util.databaseResultOf
import app.notedrop.android.util.toResultOrNotFound
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
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

    override suspend fun getNotesForVault(vaultId: String): Result<List<Note>, AppError> {
        return databaseResultOf {
            noteDao.getNotesByVaultList(vaultId).map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Result<Note, AppError> {
        return databaseResultOf {
            noteDao.getNoteById(id)
        }.andThen { entity ->
            entity.toResultOrNotFound("Note", id)
        }.map { entity ->
            entity.toDomain()
        }
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

    override suspend fun getUnsyncedNotes(): Result<List<Note>, AppError> {
        return databaseResultOf {
            noteDao.getUnsyncedNotes().map { it.toDomain() }
        }
    }

    override suspend fun getUnsyncedNotes(vaultId: String): Result<List<Note>, AppError> {
        return databaseResultOf {
            noteDao.getUnsyncedNotesByVault(vaultId).map { it.toDomain() }
        }
    }

    override suspend fun createNote(note: Note): Result<Note, AppError> {
        return databaseResultOf {
            noteDao.insertNote(note.toEntity())
            note
        }
    }

    override suspend fun updateNote(note: Note): Result<Note, AppError> {
        return databaseResultOf {
            val updatedNote = note.copy(updatedAt = Instant.now())
            noteDao.updateNote(updatedNote.toEntity())
            updatedNote
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            noteDao.deleteNoteById(id)
        }
    }

    override suspend fun deleteNotesByVault(vaultId: String): Result<Unit, AppError> {
        return databaseResultOf {
            noteDao.deleteNotesByVault(vaultId)
        }
    }

    override suspend fun syncNote(note: Note): Result<Note, AppError> {
        // TODO: Implement provider sync
        return databaseResultOf {
            val syncedNote = note.copy(isSynced = true)
            noteDao.updateNote(syncedNote.toEntity())
            syncedNote
        }
    }
}
