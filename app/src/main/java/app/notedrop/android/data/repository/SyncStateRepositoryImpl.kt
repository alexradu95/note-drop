package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.SyncStateDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.model.SyncStatus
import app.notedrop.android.domain.repository.SyncStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncStateRepository using Room database.
 */
@Singleton
class SyncStateRepositoryImpl @Inject constructor(
    private val syncStateDao: SyncStateDao
) : SyncStateRepository {

    override suspend fun getSyncState(noteId: String): SyncState? {
        return syncStateDao.getSyncState(noteId)?.toDomain()
    }

    override fun observeSyncState(noteId: String): Flow<SyncState?> {
        return syncStateDao.observeSyncState(noteId).map { it?.toDomain() }
    }

    override fun getSyncStatesForVault(vaultId: String): Flow<List<SyncState>> {
        return syncStateDao.getSyncStatesForVault(vaultId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getByStatus(status: SyncStatus): List<SyncState> {
        return syncStateDao.getByStatus(status).map { it.toDomain() }
    }

    override suspend fun getByStatusForVault(vaultId: String, status: SyncStatus): List<SyncState> {
        return syncStateDao.getByStatusForVault(vaultId, status).map { it.toDomain() }
    }

    override suspend fun getPendingUploads(vaultId: String, maxRetries: Int): List<SyncState> {
        return syncStateDao.getPendingUploads(vaultId, maxRetries).map { it.toDomain() }
    }

    override suspend fun getPendingDownloads(vaultId: String): List<SyncState> {
        return syncStateDao.getPendingDownloads(vaultId).map { it.toDomain() }
    }

    override suspend fun getConflicts(vaultId: String): List<SyncState> {
        return syncStateDao.getConflicts(vaultId).map { it.toDomain() }
    }

    override suspend fun getCountByStatus(vaultId: String, status: SyncStatus): Int {
        return syncStateDao.getCountByStatus(vaultId, status)
    }

    override suspend fun getErrorCount(vaultId: String): Int {
        return syncStateDao.getErrorCount(vaultId)
    }

    override suspend fun upsert(syncState: SyncState) {
        syncStateDao.upsert(syncState.toEntity())
    }

    override suspend fun upsertAll(syncStates: List<SyncState>) {
        syncStateDao.upsertAll(syncStates.map { it.toEntity() })
    }

    override suspend fun delete(noteId: String) {
        syncStateDao.delete(noteId)
    }

    override suspend fun deleteForVault(vaultId: String) {
        syncStateDao.deleteForVault(vaultId)
    }

    override suspend fun deleteSynced() {
        syncStateDao.deleteSynced()
    }

    override suspend fun resetRetryCountsForErrors() {
        syncStateDao.resetRetryCountsForErrors()
    }

    override suspend fun getSyncStatistics(vaultId: String): Map<SyncStatus, Int> {
        return syncStateDao.getSyncStatistics(vaultId)
            .associate { it.status to it.count }
    }
}
