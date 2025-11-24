package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.SyncStateDao
import app.notedrop.android.data.local.dao.SyncStatusCount
import app.notedrop.android.data.local.entity.SyncStateEntity
import app.notedrop.android.domain.model.SyncStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SyncStateRepositoryImplTest {

    private lateinit var syncStateDao: SyncStateDao
    private lateinit var repository: SyncStateRepositoryImpl

    @Before
    fun setUp() {
        syncStateDao = mockk()
        repository = SyncStateRepositoryImpl(syncStateDao)
    }

    @Test
    fun `getSyncState returns null when not found`() = runTest {
        // Given
        coEvery { syncStateDao.getSyncState("note1") } returns null

        // When
        val result = repository.getSyncState("note1")

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getSyncState returns domain model when found`() = runTest {
        // Given
        val entity = createTestEntity("note1", SyncStatus.SYNCED)
        coEvery { syncStateDao.getSyncState("note1") } returns entity

        // When
        val result = repository.getSyncState("note1")

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.noteId).isEqualTo("note1")
        assertThat(result?.status).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `observeSyncState emits domain models`() = runTest {
        // Given
        val entity = createTestEntity("note1", SyncStatus.PENDING_UPLOAD)
        every { syncStateDao.observeSyncState("note1") } returns flowOf(entity)

        // When
        val result = repository.observeSyncState("note1").first()

        // Then
        assertThat(result?.status).isEqualTo(SyncStatus.PENDING_UPLOAD)
    }

    @Test
    fun `getSyncStatesForVault returns list of domain models`() = runTest {
        // Given
        val entities = listOf(
            createTestEntity("note1", SyncStatus.SYNCED),
            createTestEntity("note2", SyncStatus.PENDING_UPLOAD)
        )
        every { syncStateDao.getSyncStatesForVault("vault1") } returns flowOf(entities)

        // When
        val result = repository.getSyncStatesForVault("vault1").first()

        // Then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `getByStatus filters by status`() = runTest {
        // Given
        val entities = listOf(createTestEntity("note1", SyncStatus.ERROR))
        coEvery { syncStateDao.getByStatus(SyncStatus.ERROR) } returns entities

        // When
        val result = repository.getByStatus(SyncStatus.ERROR)

        // Then
        assertThat(result).hasSize(1)
        coVerify { syncStateDao.getByStatus(SyncStatus.ERROR) }
    }

    @Test
    fun `getPendingUploads respects maxRetries parameter`() = runTest {
        // Given
        val entities = listOf(createTestEntity("note1", SyncStatus.PENDING_UPLOAD))
        coEvery { syncStateDao.getPendingUploads("vault1", 5) } returns entities

        // When
        val result = repository.getPendingUploads("vault1", maxRetries = 5)

        // Then
        assertThat(result).hasSize(1)
        coVerify { syncStateDao.getPendingUploads("vault1", 5) }
    }

    @Test
    fun `upsert converts domain to entity`() = runTest {
        // Given
        val domainModel = createTestDomainModel("note1", SyncStatus.SYNCED)
        coEvery { syncStateDao.upsert(any()) } just Runs

        // When
        repository.upsert(domainModel)

        // Then
        coVerify { syncStateDao.upsert(any()) }
    }

    @Test
    fun `upsertAll converts all domain models to entities`() = runTest {
        // Given
        val domainModels = listOf(
            createTestDomainModel("note1", SyncStatus.SYNCED),
            createTestDomainModel("note2", SyncStatus.PENDING_UPLOAD)
        )
        coEvery { syncStateDao.upsertAll(any()) } just Runs

        // When
        repository.upsertAll(domainModels)

        // Then
        coVerify { syncStateDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `delete delegates to DAO`() = runTest {
        // Given
        coEvery { syncStateDao.delete("note1") } just Runs

        // When
        repository.delete("note1")

        // Then
        coVerify { syncStateDao.delete("note1") }
    }

    @Test
    fun `deleteForVault delegates to DAO`() = runTest {
        // Given
        coEvery { syncStateDao.deleteForVault("vault1") } just Runs

        // When
        repository.deleteForVault("vault1")

        // Then
        coVerify { syncStateDao.deleteForVault("vault1") }
    }

    @Test
    fun `getSyncStatistics converts to map`() = runTest {
        // Given
        val stats = listOf(
            SyncStatusCount(SyncStatus.SYNCED, 10),
            SyncStatusCount(SyncStatus.PENDING_UPLOAD, 5)
        )
        coEvery { syncStateDao.getSyncStatistics("vault1") } returns stats

        // When
        val result = repository.getSyncStatistics("vault1")

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[SyncStatus.SYNCED]).isEqualTo(10)
        assertThat(result[SyncStatus.PENDING_UPLOAD]).isEqualTo(5)
    }

    private fun createTestEntity(
        noteId: String,
        status: SyncStatus,
        vaultId: String = "test-vault"
    ) = SyncStateEntity(
        noteId = noteId,
        vaultId = vaultId,
        status = status,
        localModifiedAt = Instant.now().toEpochMilli(),
        remoteModifiedAt = Instant.now().toEpochMilli(),
        lastSyncedAt = Instant.now().toEpochMilli(),
        remotePath = null,
        retryCount = 0,
        errorMessage = null
    )

    private fun createTestDomainModel(
        noteId: String,
        status: SyncStatus,
        vaultId: String = "test-vault"
    ) = app.notedrop.android.domain.model.SyncState(
        noteId = noteId,
        vaultId = vaultId,
        status = status,
        localModifiedAt = Instant.now(),
        remoteModifiedAt = Instant.now(),
        lastSyncedAt = Instant.now(),
        remotePath = null,
        retryCount = 0,
        errorMessage = null
    )
}
