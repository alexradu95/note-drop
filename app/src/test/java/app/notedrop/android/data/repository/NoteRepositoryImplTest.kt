package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.NoteDao
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.util.MainDispatcherRule
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for NoteRepositoryImpl with mocked DAO.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setup() {
        noteDao = mockk()
        repository = NoteRepositoryImpl(noteDao)
    }

    @Test
    fun `getAllNotes returns flow of notes`() = runTest {
        val notes = TestFixtures.createNotes(3)
        every { noteDao.getAllNotes() } returns flowOf(notes.map { it.toEntity() })

        val result = repository.getAllNotes().first()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.content }).containsExactly(
            "Test note content 1",
            "Test note content 2",
            "Test note content 3"
        )
    }

    @Test
    fun `getNotesByVault filters by vault ID`() = runTest {
        val vaultId = "vault-1"
        val notes = TestFixtures.createNotes(2).map { it.copy(vaultId = vaultId) }
        every { noteDao.getNotesByVault(vaultId) } returns flowOf(notes.map { it.toEntity() })

        val result = repository.getNotesByVault(vaultId).first()

        assertThat(result).hasSize(2)
        assertThat(result.all { it.vaultId == vaultId }).isTrue()
    }

    @Test
    fun `getNoteById returns note when exists`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.getNoteById(note.id) } returns note.toEntity()

        val result = repository.getNoteById(note.id)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(note.id)
        assertThat(result?.content).isEqualTo(note.content)
    }

    @Test
    fun `getNoteById returns null when not found`() = runTest {
        coEvery { noteDao.getNoteById(any()) } returns null

        val result = repository.getNoteById("non-existent")

        assertThat(result).isNull()
    }

    @Test
    fun `searchNotes filters by query`() = runTest {
        val query = "test"
        val matchingNotes = listOf(
            TestFixtures.createNote(content = "This is a test note"),
            TestFixtures.createNote(content = "Another test")
        )
        every { noteDao.searchNotes(query) } returns flowOf(matchingNotes.map { it.toEntity() })

        val result = repository.searchNotes(query).first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getNotesByTag returns notes with tag`() = runTest {
        val tag = "work"
        val notes = listOf(
            TestFixtures.createNote(tags = listOf("work", "important")),
            TestFixtures.createNote(tags = listOf("work"))
        )
        every { noteDao.getNotesByTag(tag) } returns flowOf(notes.map { it.toEntity() })

        val result = repository.getNotesByTag(tag).first()

        assertThat(result).hasSize(2)
        assertThat(result.all { it.tags.contains(tag) }).isTrue()
    }

    @Test
    fun `getTodaysNotes returns only today's notes`() = runTest {
        val todaysNotes = TestFixtures.createTodaysNotes(2)
        every { noteDao.getAllNotes() } returns flowOf(todaysNotes.map { it.toEntity() })

        val result = repository.getTodaysNotes().first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getUnsyncedNotes returns notes with isSynced false`() = runTest {
        val unsyncedNotes = TestFixtures.createNotes(3).map { it.copy(isSynced = false) }
        coEvery { noteDao.getUnsyncedNotes() } returns unsyncedNotes.map { it.toEntity() }

        val result = repository.getUnsyncedNotes()

        assertThat(result).hasSize(3)
        assertThat(result.all { !it.isSynced }).isTrue()
    }

    @Test
    fun `createNote inserts note successfully`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.insertNote(any()) } just Runs

        val result = repository.createNote(note)

        assertThat(result.isSuccess).isTrue()
        coVerify { noteDao.insertNote(note.toEntity()) }
    }

    @Test
    fun `createNote handles errors`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.insertNote(any()) } throws Exception("Database error")

        val result = repository.createNote(note)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(Exception::class.java)
    }

    @Test
    fun `updateNote updates existing note`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.updateNote(any()) } just Runs

        val result = repository.updateNote(note)

        assertThat(result.isSuccess).isTrue()
        coVerify { noteDao.updateNote(match { it.id == note.id }) }
    }

    @Test
    fun `updateNote updates timestamp`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.updateNote(any()) } just Runs

        val result = repository.updateNote(note)

        result.onSuccess { updatedNote ->
            assertThat(updatedNote.updatedAt.toEpochMilli())
                .isGreaterThan(note.createdAt.toEpochMilli())
        }
    }

    @Test
    fun `deleteNote removes note by ID`() = runTest {
        val noteId = "note-123"
        coEvery { noteDao.deleteNoteById(noteId) } just Runs

        val result = repository.deleteNote(noteId)

        assertThat(result.isSuccess).isTrue()
        coVerify { noteDao.deleteNoteById(noteId) }
    }

    @Test
    fun `deleteNotesByVault removes all notes in vault`() = runTest {
        val vaultId = "vault-1"
        coEvery { noteDao.deleteNotesByVault(vaultId) } just Runs

        val result = repository.deleteNotesByVault(vaultId)

        assertThat(result.isSuccess).isTrue()
        coVerify { noteDao.deleteNotesByVault(vaultId) }
    }

    @Test
    fun `syncNote marks note as synced`() = runTest {
        val note = TestFixtures.createNote(isSynced = false)
        coEvery { noteDao.updateNote(any()) } just Runs

        val result = repository.syncNote(note)

        assertThat(result.isSuccess).isTrue()
        result.onSuccess { syncedNote ->
            assertThat(syncedNote.isSynced).isTrue()
        }
    }
}
