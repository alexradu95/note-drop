package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.Vault
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for vault operations.
 */
interface VaultRepository {
    /**
     * Get all vaults as a Flow.
     */
    fun getAllVaults(): Flow<List<Vault>>

    /**
     * Get a single vault by ID.
     */
    suspend fun getVaultById(id: String): Vault?

    /**
     * Get a single vault by ID as Flow.
     */
    fun getVaultByIdFlow(id: String): Flow<Vault?>

    /**
     * Get the default vault.
     */
    suspend fun getDefaultVault(): Vault?

    /**
     * Get the default vault as Flow.
     */
    fun getDefaultVaultFlow(): Flow<Vault?>

    /**
     * Create a new vault.
     */
    suspend fun createVault(vault: Vault): Result<Vault>

    /**
     * Update an existing vault.
     */
    suspend fun updateVault(vault: Vault): Result<Vault>

    /**
     * Delete a vault.
     */
    suspend fun deleteVault(id: String): Result<Unit>

    /**
     * Set a vault as default.
     */
    suspend fun setDefaultVault(id: String): Result<Unit>

    /**
     * Update last synced timestamp.
     */
    suspend fun updateLastSynced(id: String): Result<Unit>
}
