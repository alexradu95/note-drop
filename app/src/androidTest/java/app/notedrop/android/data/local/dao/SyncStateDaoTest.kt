package app.notedrop.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.notedrop.android.data.local.NoteDropDatabase
import app.notedrop.android.data.local.entity.SyncStateEntity
import app.notedrop.android.domain.model.SyncStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class SyncStateDaoTest {

    private lateinit var database: NoteDropDatabase
    private lateinit var syncStateDao: SyncStateDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoteDropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        syncStateDao = database.syncStateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveSyncState() = runTest {
        val syncState = createTestSyncState(noteId = "1", status = SyncStatus.SYNCED)
        syncStateDao.upsert(syncState)

        val retrieved = syncStateDao.getSyncState("1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.status).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun getSyncStatesForVault_filtersCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", vaultId = "vault-A")
        val state2 = createTestSyncState(noteId = "2", vaultId = "vault-B")
        val state3 = createTestSyncState(noteId = "3", vaultId = "vault-A")

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val states = syncStateDao.getSyncStatesForVault("vault-A").first()
        assertThat(states).hasSize(2)
    }

    @Test
    fun getByStatus_filtersCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.SYNCED)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.PENDING_UPLOAD)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.SYNCED)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val synced = syncStateDao.getByStatus(SyncStatus.SYNCED)
        assertThat(synced).hasSize(2)
    }

    @Test
    fun getByStatusForVault_filtersCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", vaultId = "vault-A", status = SyncStatus.SYNCED)
        val state2 = createTestSyncState(noteId = "2", vaultId = "vault-A", status = SyncStatus.PENDING_UPLOAD)
        val state3 = createTestSyncState(noteId = "3", vaultId = "vault-B", status = SyncStatus.SYNCED)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val synced = syncStateDao.getByStatusForVault("vault-A", SyncStatus.SYNCED)
        assertThat(synced).hasSize(1)
    }

    @Test
    fun getPendingUploads_includesPendingAndErrorsWithRetries() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.PENDING_UPLOAD)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.ERROR, retryCount = 2)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.ERROR, retryCount = 3) // Max retries
        val state4 = createTestSyncState(noteId = "4", status = SyncStatus.SYNCED)

        syncStateDao.upsertAll(listOf(state1, state2, state3, state4))

        val pending = syncStateDao.getPendingUploads("test-vault", maxRetries = 3)
        assertThat(pending).hasSize(2) // state1 and state2
    }

    @Test
    fun getPendingDownloads_filtersCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.PENDING_DOWNLOAD)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.SYNCED)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.PENDING_DOWNLOAD)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val pending = syncStateDao.getPendingDownloads("test-vault")
        assertThat(pending).hasSize(2)
    }

    @Test
    fun getConflicts_filtersCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.CONFLICT)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.SYNCED)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.CONFLICT)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val conflicts = syncStateDao.getConflicts("test-vault")
        assertThat(conflicts).hasSize(2)
    }

    @Test
    fun getCountByStatus_countsCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.SYNCED)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.SYNCED)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.PENDING_UPLOAD)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val count = syncStateDao.getCountByStatus("test-vault", SyncStatus.SYNCED)
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun getErrorCount_countsCorrectly() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.ERROR)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.SYNCED)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.ERROR)

        syncStateDao.upsertAll(listOf(state1, state2, state3))

        val count = syncStateDao.getErrorCount("test-vault")
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun upsert_replacesExisting() = runTest {
        val syncState = createTestSyncState(noteId = "1", status = SyncStatus.PENDING_UPLOAD)
        syncStateDao.upsert(syncState)

        val updated = syncState.copy(status = SyncStatus.SYNCED)
        syncStateDao.upsert(updated)

        val retrieved = syncStateDao.getSyncState("1")
        assertThat(retrieved?.status).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun delete_removesSyncState() = runTest {
        val syncState = createTestSyncState(noteId = "1")
        syncStateDao.upsert(syncState)

        syncStateDao.delete("1")

        val retrieved = syncStateDao.getSyncState("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteForVault_removesAllVaultStates() = runTest {
        val state1 = createTestSyncState(noteId = "1", vaultId = "vault-A")
        val state2 = createTestSyncState(noteId = "2", vaultId = "vault-A")
        val state3 = createTestSyncState(noteId = "3", vaultId = "vault-B")

        syncStateDao.upsertAll(listOf(state1, state2, state3))
        syncStateDao.deleteForVault("vault-A")

        val remainingA = syncStateDao.getSyncStatesForVault("vault-A").first()
        val remainingB = syncStateDao.getSyncStatesForVault("vault-B").first()

        assertThat(remainingA).isEmpty()
        assertThat(remainingB).hasSize(1)
    }

    @Test
    fun deleteSynced_removesOnlySyncedStates() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.SYNCED)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.PENDING_UPLOAD)
        val state3 = createTestSyncState(noteId = "3", status = SyncStatus.SYNCED)

        syncStateDao.upsertAll(listOf(state1, state2, state3))
        syncStateDao.deleteSynced()

        val state1After = syncStateDao.getSyncState("1")
        val state2After = syncStateDao.getSyncState("2")

        assertThat(state1After).isNull()
        assertThat(state2After).isNotNull()
    }

    @Test
    fun resetRetryCountsForErrors_resetsOnlyErrors() = runTest {
        val state1 = createTestSyncState(noteId = "1", status = SyncStatus.ERROR, retryCount = 3)
        val state2 = createTestSyncState(noteId = "2", status = SyncStatus.SYNCED, retryCount = 1)

        syncStateDao.upsertAll(listOf(state1, state2))
        syncStateDao.resetRetryCountsForErrors()

        val state1After = syncStateDao.getSyncState("1")
        val state2After = syncStateDao.getSyncState("2")

        assertThat(state1After?.retryCount).isEqualTo(0)
        assertThat(state2After?.retryCount).isEqualTo(1) // Unchanged
    }

    @Test
    fun observeSyncState_emitsUpdates() = runTest {
        val syncState = createTestSyncState(noteId = "1", status = SyncStatus.PENDING_UPLOAD)
        syncStateDao.upsert(syncState)

        val observed = syncStateDao.observeSyncState("1").first()
        assertThat(observed?.status).isEqualTo(SyncStatus.PENDING_UPLOAD)
    }

    private fun createTestSyncState(
        noteId: String = "test-note",
        vaultId: String = "test-vault",
        status: SyncStatus = SyncStatus.SYNCED,
        localModifiedAt: Instant = Instant.now(),
        remoteModifiedAt: Instant? = Instant.now(),
        lastSyncedAt: Instant? = Instant.now(),
        remotePath: String? = null,
        retryCount: Int = 0,
        errorMessage: String? = null
    ) = SyncStateEntity(
        noteId = noteId,
        vaultId = vaultId,
        status = status,
        localModifiedAt = localModifiedAt,
        remoteModifiedAt = remoteModifiedAt,
        lastSyncedAt = lastSyncedAt,
        remotePath = remotePath,
        retryCount = retryCount,
        errorMessage = errorMessage
    )
}
