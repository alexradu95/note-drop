package app.notedrop.android.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.data.parser.ObsidianConfigParser
import app.notedrop.android.domain.model.ObsidianVaultConfig
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.model.toUserMessage
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val noteRepository: NoteRepository,
    private val obsidianConfigParser: ObsidianConfigParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val vaults = vaultRepository.getAllVaults()
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

    // Vault statistics flow
    val vaultStats = vaults.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    ).let { vaultsFlow ->
        kotlinx.coroutines.flow.flow {
            vaultsFlow.collect { vaultsList ->
                val statsMap = vaultsList.associate { vault ->
                    val notes = noteRepository.getNotesForVault(vault.id).getOrElse { emptyList() }
                    val unsyncedNotes = notes.filter { !it.isSynced }
                    val syncSuccessRate = if (notes.isNotEmpty()) {
                        ((notes.size - unsyncedNotes.size) * 100) / notes.size
                    } else {
                        100
                    }

                    val syncStatus = when {
                        _uiState.value.syncingVaultIds.contains(vault.id) -> app.notedrop.android.ui.settings.SyncStatus.SYNCING
                        unsyncedNotes.isEmpty() -> app.notedrop.android.ui.settings.SyncStatus.SYNCED
                        unsyncedNotes.size == notes.size && notes.isNotEmpty() -> app.notedrop.android.ui.settings.SyncStatus.ERROR
                        else -> app.notedrop.android.ui.settings.SyncStatus.UNSYNCED
                    }

                    vault.id to app.notedrop.android.ui.settings.VaultStatistics(
                        noteCount = notes.size,
                        unsyncedCount = unsyncedNotes.size,
                        syncSuccessRate = syncSuccessRate,
                        syncStatus = syncStatus
                    )
                }
                emit(statsMap)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    }

    fun createVault(
        name: String,
        description: String?,
        providerType: ProviderType,
        vaultPath: String,
        setAsDefault: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val providerConfig = when (providerType) {
                ProviderType.LOCAL -> ProviderConfig.LocalConfig(
                    storagePath = vaultPath
                )
                ProviderType.OBSIDIAN -> ProviderConfig.ObsidianConfig(
                    vaultPath = vaultPath,
                    useFrontMatter = true
                )
            }

            val vault = Vault(
                name = name,
                description = description,
                providerType = providerType,
                providerConfig = providerConfig,
                isDefault = setAsDefault
            )

            android.util.Log.d("SettingsViewModel", "Creating vault: name=$name, providerType=$providerType, vaultPath=$vaultPath, setAsDefault=$setAsDefault")

            val result = vaultRepository.createVault(vault)

            result.onSuccess {
                android.util.Log.d("SettingsViewModel", "Vault created successfully: ${vault.id}, isDefault in object=${vault.isDefault}")

                // For Obsidian vaults, parse and update config with daily notes settings
                if (providerType == ProviderType.OBSIDIAN) {
                    try {
                        val parsedConfig = obsidianConfigParser.parseVaultConfig(android.net.Uri.parse(vaultPath))
                        if (parsedConfig?.dailyNotes != null) {
                            val updatedProviderConfig = (vault.providerConfig as ProviderConfig.ObsidianConfig).copy(
                                dailyNotesPath = parsedConfig.dailyNotes.folder,
                                dailyNotesFormat = parsedConfig.dailyNotes.format
                            )
                            val updatedVault = vault.copy(providerConfig = updatedProviderConfig)
                            vaultRepository.updateVault(updatedVault).onFailure { updateError ->
                                android.util.Log.e("SettingsViewModel", "Failed to update vault config: ${updateError.toUserMessage()}")
                            }
                            android.util.Log.d("SettingsViewModel", "Updated vault with daily notes config: folder=${parsedConfig.dailyNotes.folder}, format=${parsedConfig.dailyNotes.format}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsViewModel", "Failed to parse vault config", e)
                    }
                }

                if (setAsDefault) {
                    android.util.Log.d("SettingsViewModel", "Setting vault ${vault.id} as default")
                    vaultRepository.setDefaultVault(vault.id).onFailure { setDefaultError ->
                        android.util.Log.e("SettingsViewModel", "Failed to set default vault: ${setDefaultError.toUserMessage()}")
                    }
                }
                _uiState.value = SettingsUiState(
                    isSaving = false,
                    vaultCreated = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.toUserMessage()
                )
            }
        }
    }

    fun setDefaultVault(vaultId: String) {
        viewModelScope.launch {
            android.util.Log.d("SettingsViewModel", "setDefaultVault called for: $vaultId")
            vaultRepository.setDefaultVault(vaultId)
                .onSuccess {
                    android.util.Log.d("SettingsViewModel", "setDefaultVault completed for: $vaultId")
                }
                .onFailure { error ->
                    android.util.Log.e("SettingsViewModel", "Failed to set default vault: ${error.toUserMessage()}")
                    _uiState.value = _uiState.value.copy(error = error.toUserMessage())
                }
        }
    }

    fun deleteVault(vaultId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            // First, check if there are notes in this vault
            val result = noteRepository.deleteNotesByVault(vaultId)

            result.onSuccess {
                vaultRepository.deleteVault(vaultId)
                    .onSuccess {
                        _uiState.value = SettingsUiState(isDeleting = false)
                    }
                    .onFailure { deleteError ->
                        _uiState.value = _uiState.value.copy(
                            isDeleting = false,
                            error = deleteError.toUserMessage()
                        )
                    }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = error.toUserMessage()
                )
            }
        }
    }

    fun updateVault(vault: Vault) {
        viewModelScope.launch {
            vaultRepository.updateVault(vault)
                .onFailure { error ->
                    android.util.Log.e("SettingsViewModel", "Failed to update vault: ${error.toUserMessage()}")
                    _uiState.value = _uiState.value.copy(error = error.toUserMessage())
                }
        }
    }

    fun resetState() {
        _uiState.value = SettingsUiState()
    }

    /**
     * Load Obsidian vault configuration from the vault
     */
    fun loadVaultConfig(vault: Vault) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingConfig = true)

            val providerConfig = vault.providerConfig as? ProviderConfig.ObsidianConfig
            if (providerConfig != null && providerConfig.vaultPath.isNotBlank()) {
                try {
                    val config = obsidianConfigParser.parseVaultConfig(Uri.parse(providerConfig.vaultPath))
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        vaultConfig = config,
                        currentVault = vault,
                        showConfigScreen = config != null
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Error loading vault config", e)
                    _uiState.value = _uiState.value.copy(
                        isLoadingConfig = false,
                        error = "Failed to load vault configuration"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingConfig = false,
                    error = "Invalid vault path"
                )
            }
        }
    }

    fun dismissConfigScreen() {
        _uiState.value = _uiState.value.copy(
            showConfigScreen = false,
            vaultConfig = null,
            currentVault = null
        )
    }

    /**
     * Update vault information (name, description, daily notes path)
     */
    fun updateVaultInfo(vaultId: String, name: String, description: String?, dailyNotesPath: String?) {
        viewModelScope.launch {
            vaultRepository.getVaultById(vaultId)
                .onSuccess { vault ->
                    val updatedVault = vault.copy(
                        name = name,
                        description = description,
                        providerConfig = if (vault.providerConfig is ProviderConfig.ObsidianConfig) {
                            (vault.providerConfig as ProviderConfig.ObsidianConfig).copy(
                                dailyNotesPath = dailyNotesPath
                            )
                        } else {
                            vault.providerConfig
                        }
                    )
                    vaultRepository.updateVault(updatedVault)
                        .onFailure { error ->
                            android.util.Log.e("SettingsViewModel", "Failed to update vault info: ${error.toUserMessage()}")
                            _uiState.value = _uiState.value.copy(error = error.toUserMessage())
                        }
                }
                .onFailure { error ->
                    android.util.Log.e("SettingsViewModel", "Failed to get vault: ${error.toUserMessage()}")
                    _uiState.value = _uiState.value.copy(error = error.toUserMessage())
                }
        }
    }

    /**
     * Trigger a sync for a specific vault
     */
    fun syncVault(vaultId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                syncingVaultIds = _uiState.value.syncingVaultIds + vaultId
            )

            // Get all notes for this vault
            noteRepository.getNotesForVault(vaultId)
                .onSuccess { notes ->
                    val unsyncedNotes = notes.filter { !it.isSynced }

                    // Sync each unsynced note
                    unsyncedNotes.forEach { note ->
                        // Mark note as synced (update isSynced flag)
                        noteRepository.updateNote(note.copy(isSynced = true))
                            .onFailure { error ->
                                android.util.Log.e("SettingsViewModel", "Failed to sync note ${note.id}: ${error.toUserMessage()}")
                            }
                    }

                    // Update last synced timestamp
                    vaultRepository.updateLastSynced(vaultId)
                        .onFailure { error ->
                            android.util.Log.e("SettingsViewModel", "Failed to update last synced: ${error.toUserMessage()}")
                        }

                    _uiState.value = _uiState.value.copy(
                        syncingVaultIds = _uiState.value.syncingVaultIds - vaultId
                    )
                }
                .onFailure { error ->
                    android.util.Log.e("SettingsViewModel", "Error syncing vault: ${error.toUserMessage()}")
                    _uiState.value = _uiState.value.copy(
                        syncingVaultIds = _uiState.value.syncingVaultIds - vaultId,
                        error = "Failed to sync vault: ${error.toUserMessage()}"
                    )
                }
        }
    }
}

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoadingConfig: Boolean = false,
    val vaultCreated: Boolean = false,
    val showConfigScreen: Boolean = false,
    val vaultConfig: ObsidianVaultConfig? = null,
    val currentVault: Vault? = null,
    val syncingVaultIds: Set<String> = emptySet(),
    val error: String? = null
)
