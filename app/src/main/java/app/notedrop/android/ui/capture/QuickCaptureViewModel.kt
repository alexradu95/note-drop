package app.notedrop.android.ui.capture

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.data.voice.RecordingState
import app.notedrop.android.data.voice.VoiceRecorder
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.model.TranscriptionStatus
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.model.toUserMessage
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository,
    private val templateRepository: TemplateRepository,
    private val voiceRecorder: VoiceRecorder,
    private val providerFactory: ProviderFactory,
    private val createNoteUseCase: app.notedrop.android.domain.usecase.CreateNoteUseCase
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
        // Log default vault changes
        viewModelScope.launch {
            defaultVault.collect { vault ->
                android.util.Log.d("QuickCaptureViewModel", "init - defaultVault changed: id=${vault?.id}, name=${vault?.name}")
            }
        }

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
            result.onSuccess { internalFilePath ->
                // Move file from internal storage to vault attachment folder
                val vault = defaultVault.value
                if (vault != null) {
                    val vaultRelativePath = moveAudioFileToVault(internalFilePath, vault)
                    if (vaultRelativePath != null) {
                        _uiState.value = _uiState.value.copy(voiceRecordingPath = vaultRelativePath)
                        android.util.Log.d("QuickCaptureViewModel", "Audio file moved to vault: $vaultRelativePath")
                    } else {
                        // Failed to move, keep internal path (fallback)
                        _uiState.value = _uiState.value.copy(voiceRecordingPath = internalFilePath)
                        android.util.Log.w("QuickCaptureViewModel", "Failed to move audio to vault, using internal path")
                    }
                } else {
                    // No vault configured, keep internal path
                    _uiState.value = _uiState.value.copy(voiceRecordingPath = internalFilePath)
                    android.util.Log.w("QuickCaptureViewModel", "No default vault, audio kept in internal storage")
                }
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

            // Use the unified CreateNoteUseCase
            val result = createNoteUseCase(
                content = _uiState.value.content,
                title = _uiState.value.title,
                tags = _uiState.value.tags,
                voiceRecordingPath = _uiState.value.voiceRecordingPath
            )

            result.onSuccess { savedNote ->
                android.util.Log.d("QuickCaptureViewModel", "Note saved successfully: ${savedNote.id}")

                _uiState.value = QuickCaptureUiState(
                    isSaving = false,
                    noteSaved = true
                )
            }.onFailure { error ->
                android.util.Log.e("QuickCaptureViewModel", "Failed to save note: $error")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.toUserMessage()
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = QuickCaptureUiState()
    }

    /**
     * Move audio file from internal storage to vault's attachment folder.
     * Returns the relative path within the vault, or null if failed.
     */
    private fun moveAudioFileToVault(internalFilePath: String, vault: Vault): String? {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
            if (config == null) {
                android.util.Log.e("QuickCaptureViewModel", "Vault config is not ObsidianConfig")
                return null
            }

            // Get attachment folder path from config
            val attachmentFolder = config.attachmentsPath ?: "attachments"

            // Get vault root
            val vaultUri = android.net.Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)

            if (vaultRoot == null || !vaultRoot.exists()) {
                android.util.Log.e("QuickCaptureViewModel", "Vault root not accessible")
                return null
            }

            // Find or create attachment folder
            val audioFolder = vaultRoot.findFile(attachmentFolder) ?: vaultRoot.createDirectory(attachmentFolder)

            if (audioFolder == null) {
                android.util.Log.e("QuickCaptureViewModel", "Failed to create attachment folder: $attachmentFolder")
                return null
            }

            // Get source file from internal storage
            val sourceFile = File(internalFilePath)
            if (!sourceFile.exists()) {
                android.util.Log.e("QuickCaptureViewModel", "Source file not found: $internalFilePath")
                return null
            }

            // Create destination file in vault
            val audioFileName = sourceFile.name
            val audioFile = audioFolder.createFile("audio/mp4", audioFileName)

            if (audioFile == null) {
                android.util.Log.e("QuickCaptureViewModel", "Failed to create audio file in vault")
                return null
            }

            // Copy file contents
            context.contentResolver.openOutputStream(audioFile.uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Delete original file from internal storage
            sourceFile.delete()

            // Return relative path within vault
            val relativePath = "$attachmentFolder/$audioFileName"
            android.util.Log.d("QuickCaptureViewModel", "Audio file moved to vault: $relativePath")
            relativePath

        } catch (e: Exception) {
            android.util.Log.e("QuickCaptureViewModel", "Failed to move audio file to vault", e)
            null
        }
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
