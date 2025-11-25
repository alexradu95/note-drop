package app.notedrop.android.util

import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.data.provider.ProviderCapabilities
import app.notedrop.android.data.voice.RecordingState
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Fake implementation of NoteRepository for testing.
 */
class FakeNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun getAllNotes(): Flow<List<Note>> = notes

    override fun getNotesByVault(vaultId: String): Flow<List<Note>> {
        return notes.map { it.filter { note -> note.vaultId == vaultId } }
    }

    override suspend fun getNotesForVault(vaultId: String): List<Note> {
        return notes.value.filter { it.vaultId == vaultId }
    }

    override suspend fun getNoteById(id: String): Note? {
        return notes.value.find { it.id == id }
    }

    override fun getNoteByIdFlow(id: String): Flow<Note?> {
        return notes.map { it.find { note -> note.id == id } }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        return notes.map { noteList ->
            noteList.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.title?.contains(query, ignoreCase = true) == true
            }
        }
    }

    override fun getNotesByTag(tag: String): Flow<List<Note>> {
        return notes.map { it.filter { note -> note.tags.contains(tag) } }
    }

    override fun getTodaysNotes(): Flow<List<Note>> {
        return notes.map { noteList ->
            val today = java.time.LocalDate.now()
            noteList.filter { note ->
                val noteDate = note.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                noteDate == today
            }
        }
    }

    override suspend fun getUnsyncedNotes(): List<Note> {
        return notes.value.filter { !it.isSynced }
    }

    override suspend fun getUnsyncedNotes(vaultId: String): List<Note> {
        return notes.value.filter { !it.isSynced && it.vaultId == vaultId }
    }

    override suspend fun createNote(note: Note): Result<Note> {
        notes.value = notes.value + note
        return Result.success(note)
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        notes.value = notes.value.map {
            if (it.id == note.id) note else it
        }
        return Result.success(note)
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        notes.value = notes.value.filter { it.id != id }
        return Result.success(Unit)
    }

    override suspend fun deleteNotesByVault(vaultId: String): Result<Unit> {
        notes.value = notes.value.filter { it.vaultId != vaultId }
        return Result.success(Unit)
    }

    override suspend fun syncNote(note: Note): Result<Note> {
        val syncedNote = note.copy(isSynced = true)
        notes.value = notes.value.map {
            if (it.id == note.id) syncedNote else it
        }
        return Result.success(syncedNote)
    }

    // Test helper methods
    fun setNotes(newNotes: List<Note>) {
        notes.value = newNotes
    }

    fun addNote(note: Note) {
        notes.value = notes.value + note
    }

    fun clear() {
        notes.value = emptyList()
    }
}

/**
 * Fake implementation of VaultRepository for testing.
 */
class FakeVaultRepository : VaultRepository {
    private val vaults = MutableStateFlow<List<Vault>>(emptyList())

    override fun getAllVaults(): Flow<List<Vault>> = vaults

    override suspend fun getVaultById(id: String): Vault? {
        return vaults.value.find { it.id == id }
    }

    override fun getVaultByIdFlow(id: String): Flow<Vault?> {
        return vaults.map { it.find { vault -> vault.id == id } }
    }

    override suspend fun getDefaultVault(): Vault? {
        return vaults.value.find { it.isDefault }
    }

    override fun getDefaultVaultFlow(): Flow<Vault?> {
        return vaults.map { it.find { vault -> vault.isDefault } }
    }

    override suspend fun createVault(vault: Vault): Result<Vault> {
        vaults.value = vaults.value + vault
        return Result.success(vault)
    }

    override suspend fun updateVault(vault: Vault): Result<Vault> {
        vaults.value = vaults.value.map {
            if (it.id == vault.id) vault else it
        }
        return Result.success(vault)
    }

    override suspend fun deleteVault(id: String): Result<Unit> {
        vaults.value = vaults.value.filter { it.id != id }
        return Result.success(Unit)
    }

    override suspend fun setDefaultVault(id: String): Result<Unit> {
        vaults.value = vaults.value.map {
            it.copy(isDefault = it.id == id)
        }
        return Result.success(Unit)
    }

    override suspend fun updateLastSynced(id: String): Result<Unit> {
        vaults.value = vaults.value.map {
            if (it.id == id) it.copy(lastSyncedAt = Instant.now()) else it
        }
        return Result.success(Unit)
    }

    // Test helper methods
    fun setVaults(newVaults: List<Vault>) {
        vaults.value = newVaults
    }

    fun addVault(vault: Vault) {
        vaults.value = vaults.value + vault
    }

    fun clear() {
        vaults.value = emptyList()
    }
}

/**
 * Fake implementation of TemplateRepository for testing.
 */
class FakeTemplateRepository : TemplateRepository {
    private val templates = MutableStateFlow<List<Template>>(emptyList())

    override fun getAllTemplates(): Flow<List<Template>> = templates

    override suspend fun getTemplateById(id: String): Template? {
        return templates.value.find { it.id == id }
    }

    override fun getBuiltInTemplates(): Flow<List<Template>> {
        return templates.map { it.filter { template -> template.isBuiltIn } }
    }

    override fun getUserTemplates(): Flow<List<Template>> {
        return templates.map { it.filter { template -> !template.isBuiltIn } }
    }

    override fun searchTemplates(query: String): Flow<List<Template>> {
        return templates.map { templateList ->
            templateList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun createTemplate(template: Template): Result<Template> {
        templates.value = templates.value + template
        return Result.success(template)
    }

    override suspend fun updateTemplate(template: Template): Result<Template> {
        templates.value = templates.value.map {
            if (it.id == template.id) template else it
        }
        return Result.success(template)
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> {
        templates.value = templates.value.filter { it.id != id }
        return Result.success(Unit)
    }

    override suspend fun incrementUsageCount(id: String): Result<Unit> {
        templates.value = templates.value.map {
            if (it.id == id) it.copy(usageCount = it.usageCount + 1) else it
        }
        return Result.success(Unit)
    }

    override suspend fun initializeBuiltInTemplates(): Result<Unit> {
        if (templates.value.none { it.isBuiltIn }) {
            templates.value = Template.builtInTemplates()
        }
        return Result.success(Unit)
    }

    // Test helper methods
    fun setTemplates(newTemplates: List<Template>) {
        templates.value = newTemplates
    }

    fun addTemplate(template: Template) {
        templates.value = templates.value + template
    }

    fun clear() {
        templates.value = emptyList()
    }
}

/**
 * Fake implementation of VoiceRecorder for testing.
 */
class FakeVoiceRecorder {
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private var recordingFilePath: String? = null

    fun startRecording(): Result<String> {
        recordingFilePath = "/test/recording_${System.currentTimeMillis()}.m4a"
        _recordingState.value = RecordingState.Recording(recordingFilePath!!)
        return Result.success(recordingFilePath!!)
    }

    fun stopRecording(): Result<String> {
        val path = recordingFilePath ?: return Result.failure(Exception("No recording in progress"))
        _recordingState.value = RecordingState.Idle
        return Result.success(path)
    }

    fun cancelRecording(): Result<Unit> {
        recordingFilePath = null
        _recordingState.value = RecordingState.Idle
        return Result.success(Unit)
    }

    fun pauseRecording(): Result<Unit> {
        val path = recordingFilePath ?: return Result.failure(Exception("No recording to pause"))
        _recordingState.value = RecordingState.Paused(path)
        return Result.success(Unit)
    }

    fun resumeRecording(): Result<Unit> {
        val path = recordingFilePath ?: return Result.failure(Exception("No recording to resume"))
        _recordingState.value = RecordingState.Recording(path)
        return Result.success(Unit)
    }

    // Test helper
    fun reset() {
        _recordingState.value = RecordingState.Idle
        _recordingDuration.value = 0L
        recordingFilePath = null
    }
}

/**
 * Fake implementation of NoteProvider for testing.
 */
class FakeNoteProvider : NoteProvider {
    private val savedNotes = mutableMapOf<String, Note>()
    private val remoteNotes = mutableMapOf<String, Note>()
    var shouldFail = false
    var isAvailableResult = true

    override suspend fun saveNote(note: Note, vault: Vault): Result<String> {
        return if (shouldFail) {
            Result.failure(Exception("Save failed"))
        } else {
            savedNotes[note.id] = note
            Result.success("${note.id}.md")
        }
    }

    override suspend fun loadNote(noteId: String, vault: Vault): Result<Note> {
        return (remoteNotes[noteId] ?: savedNotes[noteId])?.let {
            Result.success(it)
        } ?: Result.failure(Exception("Note not found"))
    }

    override suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit> {
        savedNotes.remove(noteId)
        remoteNotes.remove(noteId)
        return Result.success(Unit)
    }

    override suspend fun listNotes(vault: Vault): Result<List<app.notedrop.android.domain.model.NoteMetadata>> {
        return if (shouldFail) {
            Result.failure(Exception("List notes failed"))
        } else {
            val metadata = remoteNotes.values.map { note ->
                app.notedrop.android.domain.model.NoteMetadata(
                    id = note.id,
                    title = note.title,
                    path = "${note.id}.md",
                    modifiedAt = note.updatedAt,
                    size = note.content.length.toLong(),
                    tags = note.tags
                )
            }
            Result.success(metadata)
        }
    }

    override suspend fun getMetadata(noteId: String, vault: Vault): Result<app.notedrop.android.domain.model.FileMetadata> {
        val note = remoteNotes[noteId] ?: savedNotes[noteId]
            ?: return Result.failure(Exception("Note not found"))

        return Result.success(
            app.notedrop.android.domain.model.FileMetadata(
                path = "${note.id}.md",
                absolutePath = "/test/${note.id}.md",
                modifiedAt = note.updatedAt,
                size = note.content.length.toLong()
            )
        )
    }

    override suspend fun isAvailable(vault: Vault): Boolean {
        return isAvailableResult
    }

    override fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            supportsVoiceRecordings = true,
            supportsImages = true,
            supportsTags = true,
            supportsMetadata = true
        )
    }

    // Test helpers
    fun getSavedNotes(): List<Note> = savedNotes.values.toList()

    fun addRemoteNote(note: Note) {
        remoteNotes[note.id] = note
    }

    fun getRemoteNotes(): List<Note> = remoteNotes.values.toList()

    fun clear() {
        savedNotes.clear()
        remoteNotes.clear()
        shouldFail = false
        isAvailableResult = true
    }
}

/**
 * Fake implementation of SyncStateRepository for testing.
 */
class FakeSyncStateRepository : SyncStateRepository {
    private val syncStates = MutableStateFlow<Map<String, SyncState>>(emptyMap())

    override suspend fun getSyncState(noteId: String): SyncState? {
        return syncStates.value[noteId]
    }

    override fun observeSyncState(noteId: String): Flow<SyncState?> {
        return syncStates.map { it[noteId] }
    }

    override fun getSyncStatesForVault(vaultId: String): Flow<List<SyncState>> {
        return syncStates.map { states ->
            states.values.filter { it.vaultId == vaultId }
        }
    }

    override suspend fun getByStatus(status: SyncStatus): List<SyncState> {
        return syncStates.value.values.filter { it.status == status }
    }

    override suspend fun getByStatusForVault(vaultId: String, status: SyncStatus): List<SyncState> {
        return syncStates.value.values.filter {
            it.vaultId == vaultId && it.status == status
        }
    }

    override suspend fun getPendingUploads(vaultId: String, maxRetries: Int): List<SyncState> {
        return syncStates.value.values.filter {
            it.vaultId == vaultId &&
            it.status == SyncStatus.PENDING_UPLOAD &&
            it.retryCount < maxRetries
        }
    }

    override suspend fun getPendingDownloads(vaultId: String): List<SyncState> {
        return syncStates.value.values.filter {
            it.vaultId == vaultId && it.status == SyncStatus.PENDING_DOWNLOAD
        }
    }

    override suspend fun getConflicts(vaultId: String): List<SyncState> {
        return syncStates.value.values.filter {
            it.vaultId == vaultId && it.status == SyncStatus.CONFLICT
        }
    }

    override suspend fun getCountByStatus(vaultId: String, status: SyncStatus): Int {
        return syncStates.value.values.count {
            it.vaultId == vaultId && it.status == status
        }
    }

    override suspend fun getErrorCount(vaultId: String): Int {
        return syncStates.value.values.count {
            it.vaultId == vaultId && it.status == SyncStatus.ERROR
        }
    }

    override suspend fun upsert(syncState: SyncState) {
        syncStates.value = syncStates.value + (syncState.noteId to syncState)
    }

    override suspend fun upsertAll(syncStates: List<SyncState>) {
        this.syncStates.value = this.syncStates.value + syncStates.associateBy { it.noteId }
    }

    override suspend fun delete(noteId: String) {
        syncStates.value = syncStates.value - noteId
    }

    override suspend fun deleteForVault(vaultId: String) {
        syncStates.value = syncStates.value.filterValues { it.vaultId != vaultId }
    }

    override suspend fun deleteSynced() {
        syncStates.value = syncStates.value.filterValues { it.status != SyncStatus.SYNCED }
    }

    override suspend fun resetRetryCountsForErrors() {
        syncStates.value = syncStates.value.mapValues { (_, state) ->
            if (state.status == SyncStatus.ERROR) {
                state.copy(retryCount = 0)
            } else {
                state
            }
        }
    }

    override suspend fun getSyncStatistics(vaultId: String): Map<SyncStatus, Int> {
        return syncStates.value.values
            .filter { it.vaultId == vaultId }
            .groupBy { it.status }
            .mapValues { it.value.size }
    }

    // Test helpers
    fun setSyncStates(states: List<SyncState>) {
        syncStates.value = states.associateBy { it.noteId }
    }

    fun addSyncState(state: SyncState) {
        syncStates.value = syncStates.value + (state.noteId to state)
    }

    fun clear() {
        syncStates.value = emptyMap()
    }
}
