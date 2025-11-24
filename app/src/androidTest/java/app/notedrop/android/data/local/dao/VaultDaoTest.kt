package app.notedrop.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.notedrop.android.data.local.NoteDropDatabase
import app.notedrop.android.data.local.entity.VaultEntity
import app.notedrop.android.domain.model.ProviderType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class VaultDaoTest {

    private lateinit var database: NoteDropDatabase
    private lateinit var vaultDao: VaultDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoteDropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        vaultDao = database.vaultDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveVault() = runTest {
        val vault = createTestVault(id = "1", name = "Test Vault")
        vaultDao.insertVault(vault)

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo("Test Vault")
    }

    @Test
    fun getAllVaults_ordersDefaultFirst() = runTest {
        val vault1 = createTestVault(id = "1", name = "A Vault", isDefault = false)
        val vault2 = createTestVault(id = "2", name = "Z Vault", isDefault = true)
        val vault3 = createTestVault(id = "3", name = "B Vault", isDefault = false)

        vaultDao.insertVault(vault1)
        vaultDao.insertVault(vault2)
        vaultDao.insertVault(vault3)

        val vaults = vaultDao.getAllVaults().first()
        assertThat(vaults[0].id).isEqualTo("2") // Default first
        assertThat(vaults[1].name).isEqualTo("A Vault") // Then alphabetical
        assertThat(vaults[2].name).isEqualTo("B Vault")
    }

    @Test
    fun getVaultByIdFlow_emitsUpdates() = runTest {
        val vault = createTestVault(id = "1", name = "Original")
        vaultDao.insertVault(vault)

        val retrieved = vaultDao.getVaultByIdFlow("1").first()
        assertThat(retrieved?.name).isEqualTo("Original")
    }

    @Test
    fun getDefaultVault_returnsDefaultVault() = runTest {
        val vault1 = createTestVault(id = "1", isDefault = false)
        val vault2 = createTestVault(id = "2", isDefault = true)

        vaultDao.insertVault(vault1)
        vaultDao.insertVault(vault2)

        val defaultVault = vaultDao.getDefaultVault()
        assertThat(defaultVault?.id).isEqualTo("2")
    }

    @Test
    fun getDefaultVault_returnsNullWhenNoDefault() = runTest {
        val vault = createTestVault(id = "1", isDefault = false)
        vaultDao.insertVault(vault)

        val defaultVault = vaultDao.getDefaultVault()
        assertThat(defaultVault).isNull()
    }

    @Test
    fun getVaultsByType_filtersCorrectly() = runTest {
        val vault1 = createTestVault(id = "1", providerType = "OBSIDIAN")
        val vault2 = createTestVault(id = "2", providerType = "LOCAL")
        val vault3 = createTestVault(id = "3", providerType = "OBSIDIAN")

        vaultDao.insertVault(vault1)
        vaultDao.insertVault(vault2)
        vaultDao.insertVault(vault3)

        val obsidianVaults = vaultDao.getVaultsByType("OBSIDIAN").first()
        assertThat(obsidianVaults).hasSize(2)
    }

    @Test
    fun updateVault_modifiesExisting() = runTest {
        val vault = createTestVault(id = "1", name = "Original")
        vaultDao.insertVault(vault)

        val updated = vault.copy(name = "Updated")
        vaultDao.updateVault(updated)

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved?.name).isEqualTo("Updated")
    }

    @Test
    fun deleteVault_removesVault() = runTest {
        val vault = createTestVault(id = "1")
        vaultDao.insertVault(vault)

        vaultDao.deleteVault(vault)

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteVaultById_removesVault() = runTest {
        val vault = createTestVault(id = "1")
        vaultDao.insertVault(vault)

        vaultDao.deleteVaultById("1")

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun setDefaultVault_updatesDefaultStatus() = runTest {
        val vault1 = createTestVault(id = "1", isDefault = true)
        val vault2 = createTestVault(id = "2", isDefault = false)

        vaultDao.insertVault(vault1)
        vaultDao.insertVault(vault2)

        vaultDao.setDefaultVault("2")

        val vault1After = vaultDao.getVaultById("1")
        val vault2After = vaultDao.getVaultById("2")

        assertThat(vault1After?.isDefault).isFalse()
        assertThat(vault2After?.isDefault).isTrue()
    }

    @Test
    fun clearDefaultVault_removesAllDefaults() = runTest {
        val vault1 = createTestVault(id = "1", isDefault = true)
        val vault2 = createTestVault(id = "2", isDefault = false)

        vaultDao.insertVault(vault1)
        vaultDao.insertVault(vault2)

        vaultDao.clearDefaultVault()

        val vaults = vaultDao.getAllVaults().first()
        assertThat(vaults.all { !it.isDefault }).isTrue()
    }

    @Test
    fun updateLastSyncedAt_updatesTimestamp() = runTest {
        val vault = createTestVault(id = "1", lastSyncedAt = null)
        vaultDao.insertVault(vault)

        val newTimestamp = Instant.now().toEpochMilli()
        vaultDao.updateLastSyncedAt("1", newTimestamp)

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved?.lastSyncedAt).isEqualTo(newTimestamp)
    }

    @Test
    fun insertVault_replacesOnConflict() = runTest {
        val vault1 = createTestVault(id = "1", name = "Original")
        vaultDao.insertVault(vault1)

        val vault2 = createTestVault(id = "1", name = "Replaced")
        vaultDao.insertVault(vault2)

        val retrieved = vaultDao.getVaultById("1")
        assertThat(retrieved?.name).isEqualTo("Replaced")
    }

    private fun createTestVault(
        id: String = "test-id",
        name: String = "Test Vault",
        description: String? = null,
        providerType: String = ProviderType.OBSIDIAN.name,
        providerConfig: String = "{}",
        isDefault: Boolean = false,
        isEncrypted: Boolean = false,
        createdAt: Instant = Instant.now(),
        lastSyncedAt: Long? = null
    ) = VaultEntity(
        id = id,
        name = name,
        description = description,
        providerType = providerType,
        providerConfig = providerConfig,
        isDefault = isDefault,
        isEncrypted = isEncrypted,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )
}
