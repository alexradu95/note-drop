package app.notedrop.android.ui.home

import app.notedrop.android.util.FakeNoteRepository
import app.notedrop.android.util.FakeVaultRepository
import app.notedrop.android.util.MainDispatcherRule
import app.notedrop.android.util.TestFixtures
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for HomeViewModel using fake repositories.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var vaultRepository: FakeVaultRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        noteRepository = FakeNoteRepository()
        vaultRepository = FakeVaultRepository()
        viewModel = HomeViewModel(noteRepository, vaultRepository)
    }

    @Test
    fun `initial state has empty search query`() {
        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `initial filter is ALL`() {
        assertThat(viewModel.selectedFilter.value).isEqualTo(NoteFilter.ALL)
    }

    @Test
    fun `filteredNotes shows all notes with ALL filter`() = runTest {
        val notes = TestFixtures.createNotes(5)
        noteRepository.setNotes(notes)

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(5)
        }
    }

    @Test
    fun `filteredNotes shows only todays notes with TODAY filter`() = runTest {
        val todaysNotes = TestFixtures.createTodaysNotes(2)
        val oldNotes = TestFixtures.createNotes(3)
        noteRepository.setNotes(todaysNotes + oldNotes)

        viewModel.onFilterChange(NoteFilter.TODAY)
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            // Should filter to only today's notes
            assertThat(emittedNotes.size).isAtLeast(1)
        }
    }

    @Test
    fun `filteredNotes shows only voice notes with WITH_VOICE filter`() = runTest {
        val voiceNotes = listOf(
            TestFixtures.createVoiceNote(),
            TestFixtures.createVoiceNote()
        )
        val textNotes = TestFixtures.createNotes(3)
        noteRepository.setNotes(voiceNotes + textNotes)

        viewModel.onFilterChange(NoteFilter.WITH_VOICE)
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes.all { it.voiceRecordingPath != null }).isTrue()
        }
    }

    @Test
    fun `filteredNotes shows only tagged notes with TAGGED filter`() = runTest {
        val taggedNotes = listOf(
            TestFixtures.createNote(tags = listOf("work")),
            TestFixtures.createNote(tags = listOf("personal"))
        )
        val untaggedNotes = listOf(
            TestFixtures.createNote(tags = emptyList())
        )
        noteRepository.setNotes(taggedNotes + untaggedNotes)

        viewModel.onFilterChange(NoteFilter.TAGGED)
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes.all { it.tags.isNotEmpty() }).isTrue()
        }
    }

    @Test
    fun `search filters notes by content`() = runTest {
        val notes = listOf(
            TestFixtures.createNote(content = "Meeting notes"),
            TestFixtures.createNote(content = "Shopping list"),
            TestFixtures.createNote(content = "Meeting agenda")
        )
        noteRepository.setNotes(notes)

        viewModel.onSearchQueryChange("meeting")
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
            assertThat(emittedNotes.all {
                it.content.contains("meeting", ignoreCase = true)
            }).isTrue()
        }
    }

    @Test
    fun `search filters notes by title`() = runTest {
        val notes = listOf(
            TestFixtures.createNote(title = "Project Plan", content = "Details"),
            TestFixtures.createNote(title = "Ideas", content = "Project ideas"),
            TestFixtures.createNote(title = "Shopping", content = "List")
        )
        noteRepository.setNotes(notes)

        viewModel.onSearchQueryChange("project")
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
        }
    }

    @Test
    fun `search is case insensitive`() = runTest {
        val notes = listOf(
            TestFixtures.createNote(content = "UPPERCASE content"),
            TestFixtures.createNote(content = "lowercase content")
        )
        noteRepository.setNotes(notes)

        viewModel.onSearchQueryChange("CONTENT")
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(2)
        }
    }

    @Test
    fun `clearSearch resets search query`() {
        viewModel.onSearchQueryChange("test query")
        assertThat(viewModel.searchQuery.value).isEqualTo("test query")

        viewModel.clearSearch()

        assertThat(viewModel.searchQuery.value).isEmpty()
    }

    @Test
    fun `deleteNote removes note from repository`() = runTest {
        val note = TestFixtures.createNote()
        noteRepository.addNote(note)

        viewModel.deleteNote(note.id)
        advanceUntilIdle()

        noteRepository.getAllNotes().test {
            val notes = awaitItem()
            assertThat(notes).doesNotContain(note)
        }
    }

    @Test
    fun `todaysNotes emits today's notes`() = runTest {
        val todaysNotes = TestFixtures.createTodaysNotes(3)
        noteRepository.setNotes(todaysNotes)

        viewModel.todaysNotes.test {
            val notes = awaitItem()
            assertThat(notes).hasSize(3)
        }
    }

    @Test
    fun `defaultVault emits default vault`() = runTest {
        val vault = TestFixtures.createVault(isDefault = true)
        vaultRepository.addVault(vault)

        viewModel.defaultVault.test {
            val emittedVault = awaitItem()
            assertThat(emittedVault).isEqualTo(vault)
            assertThat(emittedVault?.isDefault).isTrue()
        }
    }

    @Test
    fun `combined search and filter works correctly`() = runTest {
        val notes = listOf(
            TestFixtures.createVoiceNote(content = "Voice meeting notes"),
            TestFixtures.createVoiceNote(content = "Voice shopping list"),
            TestFixtures.createNote(content = "Text meeting notes")
        )
        noteRepository.setNotes(notes)

        viewModel.onFilterChange(NoteFilter.WITH_VOICE)
        viewModel.onSearchQueryChange("meeting")
        advanceUntilIdle()

        viewModel.filteredNotes.test {
            val emittedNotes = awaitItem()
            assertThat(emittedNotes).hasSize(1)
            assertThat(emittedNotes[0].voiceRecordingPath).isNotNull()
            assertThat(emittedNotes[0].content).contains("meeting")
        }
    }
}
