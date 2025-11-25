package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.VaultDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.util.databaseResultOf
import app.notedrop.android.util.toResultOrNotFound
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
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

    override suspend fun getVaultById(id: String): Result<Vault, AppError> {
        return databaseResultOf {
            vaultDao.getVaultById(id)
        }.andThen { entity ->
            entity.toResultOrNotFound("Vault", id)
        }.map { entity ->
            entity.toDomain()
        }
    }

    override fun getVaultByIdFlow(id: String): Flow<Vault?> {
        return vaultDao.getVaultByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getDefaultVault(): Result<Vault?, AppError> {
        return databaseResultOf {
            val entity = vaultDao.getDefaultVault()
            android.util.Log.d("VaultRepositoryImpl", "getDefaultVault: entity=${entity?.id}, isDefault=${entity?.isDefault}")
            entity?.toDomain()
        }
    }

    override fun getDefaultVaultFlow(): Flow<Vault?> {
        return vaultDao.getDefaultVaultFlow().map {
            android.util.Log.d("VaultRepositoryImpl", "getDefaultVaultFlow - emitting entity: ${it?.id}, isDefault=${it?.isDefault}")
            it?.toDomain()
        }
    }

    override suspend fun createVault(vault: Vault): Result<Vault, AppError> {
        return databaseResultOf {
            val entity = vault.toEntity()
            android.util.Log.d("VaultRepositoryImpl", "Inserting vault entity: id=${entity.id}, name=${entity.name}, isDefault=${entity.isDefault}, providerConfig=${entity.providerConfig}")
            vaultDao.insertVault(entity)
            android.util.Log.d("VaultRepositoryImpl", "Vault inserted successfully")
            vault
        }
    }

    override suspend fun updateVault(vault: Vault): Result<Vault, AppError> {
        return databaseResultOf {
            vaultDao.updateVault(vault.toEntity())
            vault
        }
    }

    override suspend fun deleteVault(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            vaultDao.deleteVaultById(id)
        }
    }

    override suspend fun setDefaultVault(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            android.util.Log.d("VaultRepositoryImpl", "Setting default vault: $id")
            vaultDao.setDefaultVault(id)
            android.util.Log.d("VaultRepositoryImpl", "Default vault set successfully")

            // Verify the change
            val defaultVault = vaultDao.getDefaultVault()
            android.util.Log.d("VaultRepositoryImpl", "Current default vault after update: ${defaultVault?.id}, isDefault=${defaultVault?.isDefault}")
        }
    }

    override suspend fun updateLastSynced(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            vaultDao.updateLastSyncedAt(id, Instant.now().toEpochMilli())
        }
    }
}
