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
        val entity = vaultDao.getDefaultVault()
        android.util.Log.d("VaultRepositoryImpl", "getDefaultVault: entity=${entity?.id}, isDefault=${entity?.isDefault}")
        return entity?.toDomain()
    }

    override fun getDefaultVaultFlow(): Flow<Vault?> {
        return vaultDao.getDefaultVaultFlow().map {
            android.util.Log.d("VaultRepositoryImpl", "getDefaultVaultFlow - emitting entity: ${it?.id}, isDefault=${it?.isDefault}")
            it?.toDomain()
        }
    }

    override suspend fun createVault(vault: Vault): Result<Vault> {
        return try {
            val entity = vault.toEntity()
            android.util.Log.d("VaultRepositoryImpl", "Inserting vault entity: id=${entity.id}, name=${entity.name}, isDefault=${entity.isDefault}, providerConfig=${entity.providerConfig}")
            vaultDao.insertVault(entity)
            android.util.Log.d("VaultRepositoryImpl", "Vault inserted successfully")
            Result.success(vault)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Error creating vault", e)
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
            android.util.Log.d("VaultRepositoryImpl", "Setting default vault: $id")
            vaultDao.setDefaultVault(id)
            android.util.Log.d("VaultRepositoryImpl", "Default vault set successfully")

            // Verify the change
            val defaultVault = vaultDao.getDefaultVault()
            android.util.Log.d("VaultRepositoryImpl", "Current default vault after update: ${defaultVault?.id}, isDefault=${defaultVault?.isDefault}")

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Error setting default vault", e)
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
