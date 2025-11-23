package app.notedrop.android.util

import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
