package app.notedrop.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.notedrop.android.data.local.NoteDropDatabase
import app.notedrop.android.data.local.entity.NoteEntity
import app.notedrop.android.domain.model.TranscriptionStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Instrumented tests for NoteDao using an in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var database: NoteDropDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NoteDropDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveNote() = runTest {
        val note = createTestNote(id = "1", content = "Test note content")

        noteDao.insertNote(note)
        val retrieved = noteDao.getNoteById("1")

        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo("1")
        assertThat(retrieved?.content).isEqualTo("Test note content")
    }

    @Test
    fun getAllNotes_returnsAllNotes() = runTest {
        val note1 = createTestNote(id = "1", content = "Note 1")
        val note2 = createTestNote(id = "2", content = "Note 2")
        val note3 = createTestNote(id = "3", content = "Note 3")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val notes = noteDao.getAllNotes().first()
        assertThat(notes).hasSize(3)
    }

    @Test
    fun getAllNotes_orderedByCreatedAtDesc() = runTest {
        val now = Instant.now()
        val note1 = createTestNote(id = "1", createdAt = now.minusSeconds(120))
        val note2 = createTestNote(id = "2", createdAt = now.minusSeconds(60))
        val note3 = createTestNote(id = "3", createdAt = now)

        noteDao.insertNotes(listOf(note1, note2, note3))

        val notes = noteDao.getAllNotes().first()
        assertThat(notes[0].id).isEqualTo("3")  // Most recent first
        assertThat(notes[1].id).isEqualTo("2")
        assertThat(notes[2].id).isEqualTo("1")
    }

    @Test
    fun getNotesByVault_filtersCorrectly() = runTest {
        val note1 = createTestNote(id = "1", vaultId = "vault-A")
        val note2 = createTestNote(id = "2", vaultId = "vault-B")
        val note3 = createTestNote(id = "3", vaultId = "vault-A")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val notes = noteDao.getNotesByVault("vault-A").first()
        assertThat(notes).hasSize(2)
        assertThat(notes.map { it.id }).containsExactly("1", "3")
    }

    @Test
    fun getNoteById_returnsCorrectNote() = runTest {
        val note = createTestNote(id = "test-id", title = "Specific Note")

        noteDao.insertNote(note)
        val retrieved = noteDao.getNoteById("test-id")

        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.title).isEqualTo("Specific Note")
    }

    @Test
    fun getNoteById_returnsNullWhenNotFound() = runTest {
        val retrieved = noteDao.getNoteById("nonexistent-id")

        assertThat(retrieved).isNull()
    }

    @Test
    fun getNoteByIdFlow_emitsUpdates() = runTest {
        val note = createTestNote(id = "flow-test", content = "Original")

        noteDao.insertNote(note)

        val retrieved = noteDao.getNoteByIdFlow("flow-test").first()
        assertThat(retrieved?.content).isEqualTo("Original")
    }

    @Test
    fun searchNotes_findsByContent() = runTest {
        val note1 = createTestNote(id = "1", content = "This contains the keyword")
        val note2 = createTestNote(id = "2", content = "This does not")
        val note3 = createTestNote(id = "3", content = "Another keyword match")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val results = noteDao.searchNotes("keyword").first()
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly("1", "3")
    }

    @Test
    fun searchNotes_findsByTitle() = runTest {
        val note1 = createTestNote(id = "1", title = "Meeting Notes", content = "Content")
        val note2 = createTestNote(id = "2", title = "Daily Journal", content = "Content")
        val note3 = createTestNote(id = "3", title = "Meeting Summary", content = "Content")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val results = noteDao.searchNotes("Meeting").first()
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly("1", "3")
    }

    @Test
    fun searchNotes_isCaseInsensitive() = runTest {
        val note = createTestNote(id = "1", content = "Testing Case INSENSITIVE search")

        noteDao.insertNote(note)

        val results = noteDao.searchNotes("insensitive").first()
        assertThat(results).hasSize(1)
    }

    @Test
    fun getNotesByTag_filtersCorrectly() = runTest {
        val note1 = createTestNote(id = "1", tags = "work,important")
        val note2 = createTestNote(id = "2", tags = "personal")
        val note3 = createTestNote(id = "3", tags = "work,project")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val results = noteDao.getNotesByTag("work").first()
        assertThat(results).hasSize(2)
        assertThat(results.map { it.id }).containsExactly("1", "3")
    }

    @Test
    fun getUnsyncedNotes_returnsOnlyUnsynced() = runTest {
        val note1 = createTestNote(id = "1", isSynced = true)
        val note2 = createTestNote(id = "2", isSynced = false)
        val note3 = createTestNote(id = "3", isSynced = false)

        noteDao.insertNotes(listOf(note1, note2, note3))

        val unsynced = noteDao.getUnsyncedNotes()
        assertThat(unsynced).hasSize(2)
        assertThat(unsynced.map { it.id }).containsExactly("2", "3")
    }

    @Test
    fun getNotesCountByVault_countsCorrectly() = runTest {
        val note1 = createTestNote(id = "1", vaultId = "vault-A")
        val note2 = createTestNote(id = "2", vaultId = "vault-A")
        val note3 = createTestNote(id = "3", vaultId = "vault-B")

        noteDao.insertNotes(listOf(note1, note2, note3))

        val countA = noteDao.getNotesCountByVault("vault-A")
        val countB = noteDao.getNotesCountByVault("vault-B")

        assertThat(countA).isEqualTo(2)
        assertThat(countB).isEqualTo(1)
    }

    @Test
    fun updateNote_modifiesExistingNote() = runTest {
        val note = createTestNote(id = "1", content = "Original content")
        noteDao.insertNote(note)

        val updated = note.copy(content = "Updated content")
        noteDao.updateNote(updated)

        val retrieved = noteDao.getNoteById("1")
        assertThat(retrieved?.content).isEqualTo("Updated content")
    }

    @Test
    fun deleteNote_removesNote() = runTest {
        val note = createTestNote(id = "1")
        noteDao.insertNote(note)

        noteDao.deleteNote(note)

        val retrieved = noteDao.getNoteById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteNoteById_removesNote() = runTest {
        val note = createTestNote(id = "1")
        noteDao.insertNote(note)

        noteDao.deleteNoteById("1")

        val retrieved = noteDao.getNoteById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteNotesByVault_removesAllVaultNotes() = runTest {
        val note1 = createTestNote(id = "1", vaultId = "vault-A")
        val note2 = createTestNote(id = "2", vaultId = "vault-A")
        val note3 = createTestNote(id = "3", vaultId = "vault-B")

        noteDao.insertNotes(listOf(note1, note2, note3))
        noteDao.deleteNotesByVault("vault-A")

        val remaining = noteDao.getAllNotes().first()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].id).isEqualTo("3")
    }

    @Test
    fun insertNotes_replacesOnConflict() = runTest {
        val note1 = createTestNote(id = "1", content = "Original")
        noteDao.insertNote(note1)

        val note2 = createTestNote(id = "1", content = "Replaced")
        noteDao.insertNote(note2)

        val retrieved = noteDao.getNoteById("1")
        assertThat(retrieved?.content).isEqualTo("Replaced")
    }

    @Test
    fun markNoteAsSynced_updatesFlag() = runTest {
        val note = createTestNote(id = "1", isSynced = false)
        noteDao.insertNote(note)

        noteDao.markNoteAsSynced("1")

        val retrieved = noteDao.getNoteById("1")
        assertThat(retrieved?.isSynced).isTrue()
    }

    // Helper function to create test notes
    private fun createTestNote(
        id: String = "test-id",
        content: String = "Test content",
        title: String? = "Test Title",
        vaultId: String = "test-vault",
        tags: String = "",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        voiceRecordingPath: String? = null,
        transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NONE,
        metadata: String = "{}",
        isSynced: Boolean = false
    ) = NoteEntity(
        id = id,
        content = content,
        title = title,
        vaultId = vaultId,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        voiceRecordingPath = voiceRecordingPath,
        transcriptionStatus = transcriptionStatus,
        metadata = metadata,
        isSynced = isSynced
    )
}
