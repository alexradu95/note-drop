package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Vault
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for vault operations.
 *
 * All mutating operations return Result<T, AppError> for type-safe error handling.
 */
interface VaultRepository {
    /**
     * Get all vaults as a Flow.
     */
    fun getAllVaults(): Flow<List<Vault>>

    /**
     * Get a single vault by ID.
     */
    suspend fun getVaultById(id: String): Result<Vault, AppError>

    /**
     * Get a single vault by ID as Flow.
     */
    fun getVaultByIdFlow(id: String): Flow<Vault?>

    /**
     * Get the default vault.
     */
    suspend fun getDefaultVault(): Result<Vault?, AppError>

    /**
     * Get the default vault as Flow.
     */
    fun getDefaultVaultFlow(): Flow<Vault?>

    /**
     * Create a new vault.
     */
    suspend fun createVault(vault: Vault): Result<Vault, AppError>

    /**
     * Update an existing vault.
     */
    suspend fun updateVault(vault: Vault): Result<Vault, AppError>

    /**
     * Delete a vault.
     */
    suspend fun deleteVault(id: String): Result<Unit, AppError>

    /**
     * Set a vault as default.
     */
    suspend fun setDefaultVault(id: String): Result<Unit, AppError>

    /**
     * Update last synced timestamp.
     */
    suspend fun updateLastSynced(id: String): Result<Unit, AppError>
}
