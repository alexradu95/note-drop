package app.notedrop.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.notedrop.android.data.local.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for vaults.
 */
@Dao
interface VaultDao {
    /**
     * Get all vaults as a Flow (live updates).
     */
    @Query("SELECT * FROM vaults ORDER BY isDefault DESC, name ASC")
    fun getAllVaults(): Flow<List<VaultEntity>>

    /**
     * Get a single vault by ID.
     */
    @Query("SELECT * FROM vaults WHERE id = :id")
    suspend fun getVaultById(id: String): VaultEntity?

    /**
     * Get a single vault by ID as Flow.
     */
    @Query("SELECT * FROM vaults WHERE id = :id")
    fun getVaultByIdFlow(id: String): Flow<VaultEntity?>

    /**
     * Get the default vault.
     */
    @Query("SELECT * FROM vaults WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultVault(): VaultEntity?

    /**
     * Get the default vault as Flow.
     */
    @Query("SELECT * FROM vaults WHERE isDefault = 1 LIMIT 1")
    fun getDefaultVaultFlow(): Flow<VaultEntity?>

    /**
     * Get vaults by provider type.
     */
    @Query("SELECT * FROM vaults WHERE providerType = :type ORDER BY name ASC")
    fun getVaultsByType(type: String): Flow<List<VaultEntity>>

    /**
     * Insert a new vault.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: VaultEntity)

    /**
     * Update an existing vault.
     */
    @Update
    suspend fun updateVault(vault: VaultEntity)

    /**
     * Delete a vault.
     */
    @Delete
    suspend fun deleteVault(vault: VaultEntity)

    /**
     * Delete vault by ID.
     */
    @Query("DELETE FROM vaults WHERE id = :id")
    suspend fun deleteVaultById(id: String)

    /**
     * Clear the current default vault (set isDefault = false for all).
     */
    @Query("UPDATE vaults SET isDefault = 0")
    suspend fun clearDefaultVault()

    /**
     * Set a vault as default.
     */
    @Query("UPDATE vaults SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefaultVault(id: String)

    /**
     * Update last synced timestamp for a vault.
     */
    @Query("UPDATE vaults SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateLastSyncedAt(id: String, timestamp: Long)
}
