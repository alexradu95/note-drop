package app.notedrop.android.ui.capture

import app.notedrop.android.data.voice.RecordingState
import app.notedrop.android.data.voice.VoiceRecorder
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.sync.ProviderFactory
import app.notedrop.android.util.*
import io.mockk.every
import io.mockk.mockk
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for QuickCaptureViewModel using fake dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickCaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var vaultRepository: FakeVaultRepository
    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var voiceRecorder: VoiceRecorder
    private lateinit var fakeVoiceRecorder: FakeVoiceRecorder
    private lateinit var noteProvider: FakeNoteProvider
    private lateinit var providerFactory: ProviderFactory
    private lateinit var viewModel: QuickCaptureViewModel

    @Before
    fun setup() {
        noteRepository = FakeNoteRepository()
        vaultRepository = FakeVaultRepository()
        templateRepository = FakeTemplateRepository()
        fakeVoiceRecorder = FakeVoiceRecorder()

        // Mock VoiceRecorder to use FakeVoiceRecorder's flows
        voiceRecorder = mockk(relaxed = true)
        every { voiceRecorder.recordingState } returns fakeVoiceRecorder.recordingState
        every { voiceRecorder.recordingDuration } returns fakeVoiceRecorder.recordingDuration
        every { voiceRecorder.startRecording() } answers { fakeVoiceRecorder.startRecording() }
        every { voiceRecorder.stopRecording() } answers { fakeVoiceRecorder.stopRecording() }
        every { voiceRecorder.cancelRecording() } answers { fakeVoiceRecorder.cancelRecording() }
        every { voiceRecorder.pauseRecording() } answers { fakeVoiceRecorder.pauseRecording() }
        every { voiceRecorder.resumeRecording() } answers { fakeVoiceRecorder.resumeRecording() }

        noteProvider = FakeNoteProvider()
        providerFactory = mockk(relaxed = true)
        every { providerFactory.getProvider(any()) } returns noteProvider

        // Create a default vault for testing
        val vault = TestFixtures.createVault(isDefault = true)
        vaultRepository.addVault(vault)

        viewModel = QuickCaptureViewModel(
            noteRepository,
            vaultRepository,
            templateRepository,
            voiceRecorder,
            providerFactory
        )
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertThat(state.content).isEmpty()
        assertThat(state.title).isEmpty()
        assertThat(state.tags).isEmpty()
        assertThat(state.isSaving).isFalse()
        assertThat(state.noteSaved).isFalse()
    }

    @Test
    fun `onContentChange updates content`() {
        viewModel.onContentChange("New content")

        assertThat(viewModel.uiState.value.content).isEqualTo("New content")
    }

    @Test
    fun `onTitleChange updates title`() {
        viewModel.onTitleChange("New title")

        assertThat(viewModel.uiState.value.title).isEqualTo("New title")
    }

    @Test
    fun `onTagAdded adds unique tag`() {
        viewModel.onTagAdded("important")
        viewModel.onTagAdded("work")

        assertThat(viewModel.uiState.value.tags).containsExactly("important", "work")
    }

    @Test
    fun `onTagAdded ignores duplicate tags`() {
        viewModel.onTagAdded("important")
        viewModel.onTagAdded("important")

        assertThat(viewModel.uiState.value.tags).hasSize(1)
    }

    @Test
    fun `onTagAdded ignores blank tags`() {
        viewModel.onTagAdded("")
        viewModel.onTagAdded("  ")

        assertThat(viewModel.uiState.value.tags).isEmpty()
    }

    @Test
    fun `onTagRemoved removes tag`() {
        viewModel.onTagAdded("important")
        viewModel.onTagAdded("work")
        viewModel.onTagRemoved("important")

        assertThat(viewModel.uiState.value.tags).containsExactly("work")
    }

    @Test
    fun `onTemplateSelected updates content and template`() = runTest {
        val template = TestFixtures.createTemplate(
            name = "Test Template",
            content = "# {{title}}\n\n{{date}}"
        )
        templateRepository.addTemplate(template)

        viewModel.onTemplateSelected(template)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedTemplate).isEqualTo(template)
        assertThat(viewModel.uiState.value.content).isNotEmpty()
        assertThat(viewModel.uiState.value.content).contains("#")
    }

    @Test
    fun `onTemplateSelected increments usage count`() = runTest {
        val template = TestFixtures.createTemplate()
        templateRepository.addTemplate(template)

        viewModel.onTemplateSelected(template)
        advanceUntilIdle()

        val updatedTemplate = templateRepository.getTemplateById(template.id)
        assertThat(updatedTemplate?.usageCount).isEqualTo(1)
    }

    @Test
    fun `startRecording changes recording state`() = runTest {
        viewModel.startRecording()
        advanceUntilIdle()

        viewModel.recordingState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(RecordingState.Recording::class.java)
        }
    }

    @Test
    fun `stopRecording saves file path to state`() = runTest {
        viewModel.startRecording()
        advanceUntilIdle()

        viewModel.stopRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.voiceRecordingPath).isNotNull()
        assertThat(viewModel.uiState.value.voiceRecordingPath).contains(".m4a")
    }

    @Test
    fun `cancelRecording clears file path`() = runTest {
        viewModel.startRecording()
        advanceUntilIdle()

        viewModel.stopRecording()
        advanceUntilIdle()

        viewModel.cancelRecording()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.voiceRecordingPath).isNull()
    }

    @Test
    fun `saveNote creates note in repository`() = runTest {
        viewModel.onContentChange("Test note content")
        viewModel.onTitleChange("Test Title")
        viewModel.onTagAdded("test")

        viewModel.saveNote()
        advanceUntilIdle()

        val notes = noteRepository.getAllNotes().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().content).isEqualTo("Test note content")
            assertThat(items.first().title).isEqualTo("Test Title")
            assertThat(items.first().tags).contains("test")
        }
    }

    @Test
    fun `saveNote syncs to provider when vault available`() = runTest {
        viewModel.onContentChange("Test content")

        viewModel.saveNote()
        advanceUntilIdle()

        assertThat(noteProvider.getSavedNotes()).hasSize(1)
    }

    @Test
    fun `saveNote with voice recording sets transcription status to pending`() = runTest {
        viewModel.onContentChange("Voice note")
        viewModel.startRecording()
        advanceUntilIdle()
        viewModel.stopRecording()
        advanceUntilIdle()

        viewModel.saveNote()
        advanceUntilIdle()

        noteRepository.getAllNotes().test {
            val notes = awaitItem()
            assertThat(notes.first().transcriptionStatus.name).isEqualTo("PENDING")
        }
    }

    @Test
    fun `saveNote shows error when no default vault`() = runTest {
        vaultRepository.clear()
        viewModel.onContentChange("Test content")

        viewModel.saveNote()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).contains("vault")
    }

    @Test
    fun `saveNote sets noteSaved to true on success`() = runTest {
        viewModel.onContentChange("Test content")

        viewModel.saveNote()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.noteSaved).isTrue()
    }

    @Test
    fun `resetState clears all fields`() {
        viewModel.onContentChange("Content")
        viewModel.onTitleChange("Title")
        viewModel.onTagAdded("tag")

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertThat(state.content).isEmpty()
        assertThat(state.title).isEmpty()
        assertThat(state.tags).isEmpty()
    }
}
