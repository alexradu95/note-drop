package app.notedrop.android.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.data.voice.RecordingState
import app.notedrop.android.data.voice.VoiceRecorder
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.model.TranscriptionStatus
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for Quick Capture screen.
 */
@HiltViewModel
class QuickCaptureViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository,
    private val templateRepository: TemplateRepository,
    private val voiceRecorder: VoiceRecorder,
    private val noteProvider: NoteProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickCaptureUiState())
    val uiState: StateFlow<QuickCaptureUiState> = _uiState.asStateFlow()

    val templates = templateRepository.getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val defaultVault = vaultRepository.getDefaultVaultFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val recordingState = voiceRecorder.recordingState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecordingState.Idle
        )

    init {
        // Initialize built-in templates
        viewModelScope.launch {
            templateRepository.initializeBuiltInTemplates()
        }
    }

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
    }

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onTemplateSelected(template: Template) {
        val processedContent = processTemplate(template)
        _uiState.value = _uiState.value.copy(
            selectedTemplate = template,
            content = processedContent
        )

        // Increment usage count
        viewModelScope.launch {
            templateRepository.incrementUsageCount(template.id)
        }
    }

    fun onTagAdded(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        if (!currentTags.contains(tag) && tag.isNotBlank()) {
            currentTags.add(tag)
            _uiState.value = _uiState.value.copy(tags = currentTags)
        }
    }

    fun onTagRemoved(tag: String) {
        val currentTags = _uiState.value.tags.toMutableList()
        currentTags.remove(tag)
        _uiState.value = _uiState.value.copy(tags = currentTags)
    }

    fun startRecording() {
        viewModelScope.launch {
            voiceRecorder.startRecording()
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val result = voiceRecorder.stopRecording()
            result.onSuccess { filePath ->
                _uiState.value = _uiState.value.copy(voiceRecordingPath = filePath)
            }
        }
    }

    fun cancelRecording() {
        viewModelScope.launch {
            voiceRecorder.cancelRecording()
            _uiState.value = _uiState.value.copy(voiceRecordingPath = null)
        }
    }

    fun saveNote() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val vault = defaultVault.value
            if (vault == null) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "No default vault configured"
                )
                return@launch
            }

            val note = Note(
                content = _uiState.value.content,
                title = _uiState.value.title.takeIf { it.isNotBlank() },
                vaultId = vault.id,
                tags = _uiState.value.tags,
                voiceRecordingPath = _uiState.value.voiceRecordingPath,
                transcriptionStatus = if (_uiState.value.voiceRecordingPath != null) {
                    TranscriptionStatus.PENDING
                } else {
                    TranscriptionStatus.NONE
                }
            )

            // Save to local database
            val result = noteRepository.createNote(note)

            result.onSuccess { savedNote ->
                // Sync to provider if configured
                if (noteProvider.isAvailable(vault)) {
                    noteProvider.saveNote(savedNote, vault)
                }

                _uiState.value = QuickCaptureUiState(
                    isSaving = false,
                    noteSaved = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = QuickCaptureUiState()
    }

    /**
     * Process template variables.
     */
    private fun processTemplate(template: Template): String {
        var content = template.content

        // Replace common variables
        val now = Instant.now()
        val zonedDateTime = now.atZone(ZoneId.systemDefault())

        content = content.replace("{{date}}", LocalDate.now().toString())
        content = content.replace("{{time}}", zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
        content = content.replace("{{datetime}}", zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        content = content.replace("{{title}}", _uiState.value.title)

        return content
    }
}

/**
 * UI state for Quick Capture screen.
 */
data class QuickCaptureUiState(
    val content: String = "",
    val title: String = "",
    val tags: List<String> = emptyList(),
    val selectedTemplate: Template? = null,
    val voiceRecordingPath: String? = null,
    val isSaving: Boolean = false,
    val noteSaved: Boolean = false,
    val error: String? = null
)
