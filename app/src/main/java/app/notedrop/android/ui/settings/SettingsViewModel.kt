package app.notedrop.android.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.notedrop.android.data.parser.ObsidianConfigParser
import app.notedrop.android.domain.model.ObsidianVaultConfig
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
                            vaultRepository.updateVault(updatedVault)
                            android.util.Log.d("SettingsViewModel", "Updated vault with daily notes config: folder=${parsedConfig.dailyNotes.folder}, format=${parsedConfig.dailyNotes.format}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsViewModel", "Failed to parse vault config", e)
                    }
                }

                if (setAsDefault) {
                    android.util.Log.d("SettingsViewModel", "Setting vault ${vault.id} as default")
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
            android.util.Log.d("SettingsViewModel", "setDefaultVault called for: $vaultId")
            vaultRepository.setDefaultVault(vaultId)
            android.util.Log.d("SettingsViewModel", "setDefaultVault completed for: $vaultId")
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
    val error: String? = null
)
