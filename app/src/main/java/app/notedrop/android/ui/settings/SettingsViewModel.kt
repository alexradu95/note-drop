package app.notedrop.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
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
    private val noteRepository: NoteRepository
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
                ProviderType.NOTION -> ProviderConfig.NotionConfig(
                    workspaceId = vaultPath
                )
                ProviderType.CUSTOM -> ProviderConfig.CustomConfig(
                    config = emptyMap()
                )
            }

            val vault = Vault(
                name = name,
                description = description,
                providerType = providerType,
                providerConfig = providerConfig,
                isDefault = setAsDefault
            )

            val result = vaultRepository.createVault(vault)

            result.onSuccess {
                if (setAsDefault) {
                    vaultRepository.setDefaultVault(vault.id)
                }
                _uiState.value = SettingsUiState(
                    isSaving = false,
                    vaultCreated = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = error.message
                )
            }
        }
    }

    fun setDefaultVault(vaultId: String) {
        viewModelScope.launch {
            vaultRepository.setDefaultVault(vaultId)
        }
    }

    fun deleteVault(vaultId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            // First, check if there are notes in this vault
            val result = noteRepository.deleteNotesByVault(vaultId)

            result.onSuccess {
                vaultRepository.deleteVault(vaultId)
                _uiState.value = SettingsUiState(isDeleting = false)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = error.message
                )
            }
        }
    }

    fun updateVault(vault: Vault) {
        viewModelScope.launch {
            vaultRepository.updateVault(vault)
        }
    }

    fun resetState() {
        _uiState.value = SettingsUiState()
    }
}

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val vaultCreated: Boolean = false,
    val error: String? = null
)
