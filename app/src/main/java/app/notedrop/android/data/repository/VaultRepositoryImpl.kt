package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.VaultDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VaultRepository.
 */
@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultDao: VaultDao
) : VaultRepository {

    override fun getAllVaults(): Flow<List<Vault>> {
        return vaultDao.getAllVaults().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getVaultById(id: String): Vault? {
        return vaultDao.getVaultById(id)?.toDomain()
    }

    override fun getVaultByIdFlow(id: String): Flow<Vault?> {
        return vaultDao.getVaultByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getDefaultVault(): Vault? {
        return vaultDao.getDefaultVault()?.toDomain()
    }

    override fun getDefaultVaultFlow(): Flow<Vault?> {
        return vaultDao.getDefaultVaultFlow().map { it?.toDomain() }
    }

    override suspend fun createVault(vault: Vault): Result<Vault> {
        return try {
            vaultDao.insertVault(vault.toEntity())
            Result.success(vault)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVault(vault: Vault): Result<Vault> {
        return try {
            vaultDao.updateVault(vault.toEntity())
            Result.success(vault)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVault(id: String): Result<Unit> {
        return try {
            vaultDao.deleteVaultById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setDefaultVault(id: String): Result<Unit> {
        return try {
            vaultDao.setDefaultVault(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLastSynced(id: String): Result<Unit> {
        return try {
            vaultDao.updateLastSyncedAt(id, Instant.now().toEpochMilli())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
