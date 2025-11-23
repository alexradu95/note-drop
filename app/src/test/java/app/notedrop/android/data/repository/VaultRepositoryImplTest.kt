package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.VaultDao
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.util.MainDispatcherRule
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for VaultRepositoryImpl with mocked DAO.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var vaultDao: VaultDao
    private lateinit var repository: VaultRepositoryImpl

    @Before
    fun setup() {
        vaultDao = mockk()
        repository = VaultRepositoryImpl(vaultDao)
    }

    @Test
    fun `getAllVaults returns flow of vaults`() = runTest {
        val vaults = TestFixtures.createVaults(3)
        every { vaultDao.getAllVaults() } returns flowOf(vaults.map { it.toEntity() })

        val result = repository.getAllVaults().first()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.name }).containsExactly(
            "Test Vault 1",
            "Test Vault 2",
            "Test Vault 3"
        )
    }

    @Test
    fun `getVaultById returns vault when exists`() = runTest {
        val vault = TestFixtures.createVault()
        coEvery { vaultDao.getVaultById(vault.id) } returns vault.toEntity()

        val result = repository.getVaultById(vault.id)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(vault.id)
        assertThat(result?.name).isEqualTo(vault.name)
    }

    @Test
    fun `getVaultById returns null when not found`() = runTest {
        coEvery { vaultDao.getVaultById(any()) } returns null

        val result = repository.getVaultById("non-existent")

        assertThat(result).isNull()
    }

    @Test
    fun `getVaultByIdFlow returns flow of vault`() = runTest {
        val vault = TestFixtures.createVault()
        every { vaultDao.getVaultByIdFlow(vault.id) } returns flowOf(vault.toEntity())

        val result = repository.getVaultByIdFlow(vault.id).first()

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(vault.id)
    }

    @Test
    fun `getDefaultVault returns default vault when exists`() = runTest {
        val vault = TestFixtures.createVault(isDefault = true)
        coEvery { vaultDao.getDefaultVault() } returns vault.toEntity()

        val result = repository.getDefaultVault()

        assertThat(result).isNotNull()
        assertThat(result?.isDefault).isTrue()
    }

    @Test
    fun `getDefaultVaultFlow returns flow of default vault`() = runTest {
        val vault = TestFixtures.createVault(isDefault = true)
        every { vaultDao.getDefaultVaultFlow() } returns flowOf(vault.toEntity())

        val result = repository.getDefaultVaultFlow().first()

        assertThat(result).isNotNull()
        assertThat(result?.isDefault).isTrue()
    }

    @Test
    fun `createVault inserts vault successfully`() = runTest {
        val vault = TestFixtures.createVault()
        coEvery { vaultDao.insertVault(any()) } just Runs

        val result = repository.createVault(vault)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(vault)
        coVerify { vaultDao.insertVault(vault.toEntity()) }
    }

    @Test
    fun `createVault handles errors`() = runTest {
        val vault = TestFixtures.createVault()
        val exception = Exception("Database error")
        coEvery { vaultDao.insertVault(any()) } throws exception

        val result = repository.createVault(vault)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `updateVault updates existing vault`() = runTest {
        val vault = TestFixtures.createVault()
        coEvery { vaultDao.updateVault(any()) } just Runs

        val result = repository.updateVault(vault)

        assertThat(result.isSuccess).isTrue()
        coVerify { vaultDao.updateVault(vault.toEntity()) }
    }

    @Test
    fun `deleteVault removes vault by ID`() = runTest {
        val vaultId = "vault-1"
        coEvery { vaultDao.deleteVaultById(vaultId) } just Runs

        val result = repository.deleteVault(vaultId)

        assertThat(result.isSuccess).isTrue()
        coVerify { vaultDao.deleteVaultById(vaultId) }
    }

    @Test
    fun `setDefaultVault sets vault as default`() = runTest {
        val vaultId = "vault-1"
        coEvery { vaultDao.setDefaultVault(vaultId) } just Runs

        val result = repository.setDefaultVault(vaultId)

        assertThat(result.isSuccess).isTrue()
        coVerify { vaultDao.setDefaultVault(vaultId) }
    }

    @Test
    fun `updateLastSynced updates sync timestamp`() = runTest {
        val vaultId = "vault-1"
        coEvery { vaultDao.updateLastSyncedAt(vaultId, any()) } just Runs

        val result = repository.updateLastSynced(vaultId)

        assertThat(result.isSuccess).isTrue()
        coVerify { vaultDao.updateLastSyncedAt(vaultId, any()) }
    }
}
