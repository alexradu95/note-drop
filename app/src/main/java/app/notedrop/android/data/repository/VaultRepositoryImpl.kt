package app.notedrop.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Vault
import app.notedrop.android.domain.repository.VaultRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VAULT-ONLY: Implementation of VaultRepository using DataStore.
 *
 * Stores vault configurations in DataStore preferences instead of Room database.
 * This is simpler and more appropriate for vault-only architecture.
 */
@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : VaultRepository {

    private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "vaults")

    companion object {
        private val VAULTS_KEY = stringPreferencesKey("vaults_json")
        private val DEFAULT_VAULT_ID_KEY = stringPreferencesKey("default_vault_id")
    }

    private suspend fun readVaults(): List<Vault> {
        return try {
            val prefs = context.vaultDataStore.data.first()
            val vaultsJson = prefs[VAULTS_KEY] ?: return emptyList()
            val type = object : TypeToken<List<Vault>>() {}.type
            gson.fromJson<List<Vault>>(vaultsJson, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to read vaults", e)
            emptyList()
        }
    }

    private suspend fun writeVaults(vaults: List<Vault>) {
        try {
            val json = gson.toJson(vaults)
            android.util.Log.d("VaultRepositoryImpl", "Serializing ${vaults.size} vaults to JSON (${json.length} chars)")
            android.util.Log.v("VaultRepositoryImpl", "JSON: $json")

            context.vaultDataStore.edit { prefs ->
                prefs[VAULTS_KEY] = json
            }

            android.util.Log.d("VaultRepositoryImpl", "Successfully wrote vaults to DataStore")
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to write vaults", e)
            e.printStackTrace()
            throw e
        }
    }

    override fun getAllVaults(): Flow<List<Vault>> {
        return context.vaultDataStore.data.map { prefs ->
            try {
                val vaultsJson = prefs[VAULTS_KEY] ?: return@map emptyList()
                val type = object : TypeToken<List<Vault>>() {}.type
                gson.fromJson<List<Vault>>(vaultsJson, type) ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("VaultRepositoryImpl", "Failed to decode vaults", e)
                emptyList()
            }
        }
    }

    override suspend fun getVaultById(id: String): Result<Vault, AppError> {
        return try {
            val vaults = readVaults()
            val vault = vaults.find { it.id == id }
            if (vault != null) {
                Ok(vault)
            } else {
                Err(AppError.Database.NotFound("Vault", id))
            }
        } catch (e: Exception) {
            Err(AppError.Unknown("Failed to get vault: ${e.message}", e))
        }
    }

    override fun getVaultByIdFlow(id: String): Flow<Vault?> {
        return getAllVaults().map { vaults ->
            vaults.find { it.id == id }
        }
    }

    override suspend fun getDefaultVault(): Result<Vault?, AppError> {
        return try {
            val prefs = context.vaultDataStore.data.first()
            val defaultVaultId = prefs[DEFAULT_VAULT_ID_KEY]

            if (defaultVaultId == null) {
                android.util.Log.d("VaultRepositoryImpl", "No default vault ID set")
                return Ok(null)
            }

            val vaults = readVaults()
            val vault = vaults.find { it.id == defaultVaultId }
            android.util.Log.d("VaultRepositoryImpl", "getDefaultVault: id=$defaultVaultId, found=${vault != null}")
            Ok(vault)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to get default vault", e)
            Err(AppError.Unknown("Failed to get default vault: ${e.message}", e))
        }
    }

    override fun getDefaultVaultFlow(): Flow<Vault?> {
        return context.vaultDataStore.data.map { prefs ->
            try {
                val defaultVaultId = prefs[DEFAULT_VAULT_ID_KEY]
                if (defaultVaultId == null) {
                    null
                } else {
                    val vaultsJson = prefs[VAULTS_KEY] ?: return@map null
                    val type = object : TypeToken<List<Vault>>() {}.type
                    val vaults = gson.fromJson<List<Vault>>(vaultsJson, type) ?: return@map null
                    vaults.find { it.id == defaultVaultId }
                }
            } catch (e: Exception) {
                android.util.Log.e("VaultRepositoryImpl", "Failed to get default vault flow", e)
                null
            }
        }
    }

    override suspend fun createVault(vault: Vault): Result<Vault, AppError> {
        return try {
            android.util.Log.d("VaultRepositoryImpl", "createVault called: id=${vault.id}, name=${vault.name}, isDefault=${vault.isDefault}")

            val vaults = readVaults().toMutableList()
            android.util.Log.d("VaultRepositoryImpl", "Read ${vaults.size} existing vaults")

            // If this is marked as default or no other vault exists, set it as default
            if (vault.isDefault || vaults.isEmpty()) {
                android.util.Log.d("VaultRepositoryImpl", "Setting as default vault (isDefault=${vault.isDefault}, isEmpty=${vaults.isEmpty()})")

                // Unmark other vaults as default
                val updatedVaults = vaults.map { it.copy(isDefault = false) }.toMutableList()
                updatedVaults.add(vault.copy(isDefault = true))

                android.util.Log.d("VaultRepositoryImpl", "Writing ${updatedVaults.size} vaults to DataStore")
                writeVaults(updatedVaults)

                // Set default vault ID
                context.vaultDataStore.edit { prefs ->
                    prefs[DEFAULT_VAULT_ID_KEY] = vault.id
                    android.util.Log.d("VaultRepositoryImpl", "Set default vault ID to ${vault.id}")
                }

                android.util.Log.d("VaultRepositoryImpl", "Created vault ${vault.id} and set as default - SUCCESS")
                Ok(vault.copy(isDefault = true))
            } else {
                android.util.Log.d("VaultRepositoryImpl", "Adding as non-default vault")
                vaults.add(vault)
                writeVaults(vaults)
                android.util.Log.d("VaultRepositoryImpl", "Created vault ${vault.id} - SUCCESS")
                Ok(vault)
            }
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to create vault: ${e.message}", e)
            e.printStackTrace()
            Err(AppError.Database.InsertError("Failed to create vault: ${e.message}", e))
        }
    }

    override suspend fun updateVault(vault: Vault): Result<Vault, AppError> {
        return try {
            val vaults = readVaults().toMutableList()
            val index = vaults.indexOfFirst { it.id == vault.id }

            if (index == -1) {
                return Err(AppError.Database.NotFound("Vault", vault.id))
            }

            vaults[index] = vault
            writeVaults(vaults)

            android.util.Log.d("VaultRepositoryImpl", "Updated vault ${vault.id}")
            Ok(vault)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to update vault", e)
            Err(AppError.Database.UpdateError("Failed to update vault: ${e.message}", e))
        }
    }

    override suspend fun deleteVault(id: String): Result<Unit, AppError> {
        return try {
            val vaults = readVaults().toMutableList()
            val removed = vaults.removeIf { it.id == id }

            if (!removed) {
                return Err(AppError.Database.NotFound("Vault", id))
            }

            writeVaults(vaults)

            // If deleted vault was default, clear default
            val prefs = context.vaultDataStore.data.first()
            if (prefs[DEFAULT_VAULT_ID_KEY] == id) {
                context.vaultDataStore.edit { it.remove(DEFAULT_VAULT_ID_KEY) }
            }

            android.util.Log.d("VaultRepositoryImpl", "Deleted vault $id")
            Ok(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to delete vault", e)
            Err(AppError.Database.DeleteError("Failed to delete vault: ${e.message}", e))
        }
    }

    override suspend fun setDefaultVault(id: String): Result<Unit, AppError> {
        return try {
            val vaults = readVaults().toMutableList()
            val vaultIndex = vaults.indexOfFirst { it.id == id }

            if (vaultIndex == -1) {
                return Err(AppError.Database.NotFound("Vault", id))
            }

            // Update all vaults - unmark all as default, mark target as default
            val updatedVaults = vaults.mapIndexed { index, vault ->
                vault.copy(isDefault = index == vaultIndex)
            }

            writeVaults(updatedVaults)

            // Set default vault ID
            context.vaultDataStore.edit { prefs ->
                prefs[DEFAULT_VAULT_ID_KEY] = id
            }

            android.util.Log.d("VaultRepositoryImpl", "Set default vault to $id")
            Ok(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to set default vault", e)
            Err(AppError.Database.UpdateError("Failed to set default vault: ${e.message}", e))
        }
    }

    override suspend fun updateLastSynced(id: String): Result<Unit, AppError> {
        return try {
            val vaults = readVaults().toMutableList()
            val index = vaults.indexOfFirst { it.id == id }

            if (index == -1) {
                return Err(AppError.Database.NotFound("Vault", id))
            }

            vaults[index] = vaults[index].copy(lastSyncedAt = Instant.now())
            writeVaults(vaults)

            android.util.Log.d("VaultRepositoryImpl", "Updated last synced for vault $id")
            Ok(Unit)
        } catch (e: Exception) {
            android.util.Log.e("VaultRepositoryImpl", "Failed to update last synced", e)
            Err(AppError.Database.UpdateError("Failed to update last synced: ${e.message}", e))
        }
    }
}
