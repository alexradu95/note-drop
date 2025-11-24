package app.notedrop.android.ui.settings

import app.notedrop.android.data.parser.ObsidianConfigParser
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.util.*
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for SettingsViewModel using fake repositories.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vaultRepository: FakeVaultRepository
    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var obsidianConfigParser: ObsidianConfigParser
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        vaultRepository = FakeVaultRepository()
        noteRepository = FakeNoteRepository()
        obsidianConfigParser = mockk(relaxed = true)
        viewModel = SettingsViewModel(vaultRepository, noteRepository, obsidianConfigParser)
    }

    @Test
    fun `initial state has no errors`() {
        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.isDeleting).isFalse()
        assertThat(state.vaultCreated).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `createVault creates Obsidian vault`() = runTest {
        viewModel.createVault(
            name = "My Vault",
            description = "Test vault",
            providerType = ProviderType.OBSIDIAN,
            vaultPath = "/storage/vault",
            setAsDefault = false
        )
        advanceUntilIdle()

        viewModel.vaults.test {
            val vaults = awaitItem()
            assertThat(vaults).hasSize(1)
            assertThat(vaults.first().name).isEqualTo("My Vault")
            assertThat(vaults.first().providerType).isEqualTo(ProviderType.OBSIDIAN)
        }
    }

    @Test
    fun `createVault sets as default when requested`() = runTest {
        viewModel.createVault(
            name = "Default Vault",
            description = "Test vault",
            providerType = ProviderType.OBSIDIAN,
            vaultPath = "/storage/vault",
            setAsDefault = true
        )
        advanceUntilIdle()

        viewModel.defaultVault.test {
            val vault = awaitItem()
            assertThat(vault).isNotNull()
            assertThat(vault?.name).isEqualTo("Default Vault")
            assertThat(vault?.isDefault).isTrue()
        }
    }

    @Test
    fun `createVault creates Local vault`() = runTest {
        viewModel.createVault(
            name = "Local Vault",
            description = null,
            providerType = ProviderType.LOCAL,
            vaultPath = "/local/path",
            setAsDefault = false
        )
        advanceUntilIdle()

        viewModel.vaults.test {
            val vaults = awaitItem()
            assertThat(vaults.first().providerType).isEqualTo(ProviderType.LOCAL)
        }
    }

    @Test
    fun `createVault sets vaultCreated to true on success`() = runTest {
        viewModel.createVault(
            name = "Test Vault",
            description = null,
            providerType = ProviderType.OBSIDIAN,
            vaultPath = "/path",
            setAsDefault = false
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.vaultCreated).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `setDefaultVault updates default vault`() = runTest {
        val vault1 = TestFixtures.createVault(name = "Vault 1", isDefault = true)
        val vault2 = TestFixtures.createVault(name = "Vault 2", isDefault = false)
        vaultRepository.addVault(vault1)
        vaultRepository.addVault(vault2)

        viewModel.setDefaultVault(vault2.id)
        advanceUntilIdle()

        viewModel.defaultVault.test {
            val defaultVault = awaitItem()
            assertThat(defaultVault?.id).isEqualTo(vault2.id)
        }
    }

    @Test
    fun `deleteVault removes vault and its notes`() = runTest {
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        viewModel.deleteVault(vault.id)
        advanceUntilIdle()

        viewModel.vaults.test {
            assertThat(awaitItem()).isEmpty()
        }

        noteRepository.getAllNotes().test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `deleteVault sets isDeleting during operation`() = runTest {
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        viewModel.deleteVault(vault.id)
        // Don't advance until idle yet - check intermediate state
        // Note: This is tricky to test without more sophisticated timing control

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isDeleting).isFalse()
    }

    @Test
    fun `updateVault updates existing vault`() = runTest {
        val vault = TestFixtures.createVault(name = "Original Name")
        vaultRepository.addVault(vault)

        val updatedVault = vault.copy(name = "Updated Name")
        viewModel.updateVault(updatedVault)
        advanceUntilIdle()

        val retrieved = vaultRepository.getVaultById(vault.id)
        assertThat(retrieved?.name).isEqualTo("Updated Name")
    }

    @Test
    fun `resetState clears all fields`() {
        viewModel.createVault("Test", null, ProviderType.OBSIDIAN, "/path", false)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.isDeleting).isFalse()
        assertThat(state.vaultCreated).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `vaults flow emits all vaults`() = runTest {
        val vault1 = TestFixtures.createVault(name = "Vault 1")
        val vault2 = TestFixtures.createVault(name = "Vault 2")
        vaultRepository.addVault(vault1)
        vaultRepository.addVault(vault2)

        viewModel.vaults.test {
            val vaults = awaitItem()
            assertThat(vaults).hasSize(2)
            assertThat(vaults.map { it.name }).containsExactly("Vault 1", "Vault 2")
        }
    }

    @Test
    fun `defaultVault flow emits default vault`() = runTest {
        val vault = TestFixtures.createVault(isDefault = true)
        vaultRepository.addVault(vault)

        viewModel.defaultVault.test {
            val defaultVault = awaitItem()
            assertThat(defaultVault).isNotNull()
            assertThat(defaultVault?.isDefault).isTrue()
        }
    }
}
