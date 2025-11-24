package app.notedrop.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen showing notes and daily notes.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository,
    private val syncStateRepository: SyncStateRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(NoteFilter.ALL)
    val selectedFilter: StateFlow<NoteFilter> = _selectedFilter.asStateFlow()

    private val _selectedVault = MutableStateFlow<app.notedrop.android.domain.model.Vault?>(null)
    val selectedVault: StateFlow<app.notedrop.android.domain.model.Vault?> = _selectedVault.asStateFlow()

    // All vaults
    val allVaults = vaultRepository.getAllVaults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Default vault
    val defaultVault = vaultRepository.getDefaultVaultFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // All notes from the repository (filtered by selected vault)
    private val allNotes = combine(
        _selectedVault,
        defaultVault
    ) { selected, default ->
        val activeVault = selected ?: default
        activeVault?.id
    }.flatMapLatest { vaultId ->
        if (vaultId != null) {
            noteRepository.getNotesByVault(vaultId)
        } else {
            noteRepository.getAllNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's notes
    val todaysNotes = noteRepository.getTodaysNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sync states for all notes (noteId -> SyncState)
    val syncStatesMap = combine(
        _selectedVault,
        defaultVault
    ) { selected, default ->
        val activeVault = selected ?: default
        activeVault?.id
    }.flatMapLatest { vaultId ->
        if (vaultId != null) {
            syncStateRepository.getSyncStatesForVault(vaultId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.map { states ->
        states.associateBy { it.noteId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Filtered notes based on search and filter
    val filteredNotes = combine(
        allNotes,
        _searchQuery,
        _selectedFilter
    ) { notes, query, filter ->
        var filtered = notes

        // Apply filter
        filtered = when (filter) {
            NoteFilter.ALL -> filtered
            NoteFilter.TODAY -> filtered.filter { note ->
                todaysNotes.value.contains(note)
            }
            NoteFilter.WITH_VOICE -> filtered.filter { it.voiceRecordingPath != null }
            NoteFilter.TAGGED -> filtered.filter { it.tags.isNotEmpty() }
        }

        // Apply search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { note ->
                note.content.contains(query, ignoreCase = true) ||
                note.title?.contains(query, ignoreCase = true) == true ||
                note.tags.any { it.contains(query, ignoreCase = true) }
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: NoteFilter) {
        _selectedFilter.value = filter
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun selectVault(vault: app.notedrop.android.domain.model.Vault) {
        _selectedVault.value = vault
    }

    /**
     * Get sync state for a specific note.
     */
    fun getSyncState(noteId: String): SyncState? {
        return syncStatesMap.value[noteId]
    }
}

/**
 * Filter options for notes.
 */
enum class NoteFilter {
    ALL,
    TODAY,
    WITH_VOICE,
    TAGGED
}
