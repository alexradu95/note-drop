package app.notedrop.android.data.vault

import android.content.Context
import android.net.Uri
import app.notedrop.android.data.parser.ObsidianConfigParser
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.ObsidianVaultConfig
import app.notedrop.android.domain.model.ProviderConfig
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads vault configuration directly from the vault folder.
 *
 * This is the vault-only approach - configuration is read from app.json
 * and daily-notes.json on-demand, not cached in database.
 *
 * Usage:
 * ```
 * val config = vaultConfigReader.readVaultConfig(vaultUri)
 * config.onSuccess { obsidianConfig ->
 *     // Use config
 * }
 * ```
 */
@Singleton
class VaultConfigReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configParser: ObsidianConfigParser
) {
    /**
     * Read configuration from vault and convert to ProviderConfig.ObsidianConfig
     *
     * @param vaultUri The content URI of the vault root folder
     * @return Result containing ObsidianConfig or error
     */
    fun readVaultConfig(vaultUri: Uri): Result<ProviderConfig.ObsidianConfig, AppError> {
        return try {
            val vaultConfig = configParser.parseVaultConfig(vaultUri)

            if (vaultConfig == null) {
                // Vault config couldn't be parsed - return defaults
                android.util.Log.w(TAG, "Could not parse vault config, using defaults")
                return Ok(createDefaultConfig(vaultUri))
            }

            Ok(convertToObsidianConfig(vaultUri, vaultConfig))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to read vault config", e)
            Err(AppError.FileSystem.ReadError(vaultUri.toString(), e))
        }
    }

    /**
     * Read just the vault configuration structure (for display/debugging).
     *
     * @param vaultUri The content URI of the vault root folder
     * @return Result containing ObsidianVaultConfig or error
     */
    fun readRawVaultConfig(vaultUri: Uri): Result<ObsidianVaultConfig, AppError> {
        return try {
            val vaultConfig = configParser.parseVaultConfig(vaultUri)

            if (vaultConfig == null) {
                return Err(AppError.FileSystem.NotFound(vaultUri.toString()))
            }

            Ok(vaultConfig)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to read vault config", e)
            Err(AppError.FileSystem.ReadError(vaultUri.toString(), e))
        }
    }

    /**
     * Convert ObsidianVaultConfig to ProviderConfig.ObsidianConfig
     */
    private fun convertToObsidianConfig(
        vaultUri: Uri,
        vaultConfig: ObsidianVaultConfig
    ): ProviderConfig.ObsidianConfig {
        return ProviderConfig.ObsidianConfig(
            vaultPath = vaultUri.toString(),
            dailyNotesPath = vaultConfig.dailyNotes?.folder,
            dailyNotesFormat = vaultConfig.dailyNotes?.format,
            templatePath = vaultConfig.dailyNotes?.template,
            attachmentsPath = vaultConfig.app?.attachmentFolderPath ?: "attachments",

            // Markdown format settings - use sensible defaults
            useFrontMatter = true,
            frontMatterTemplate = null,
            preserveObsidianLinks = true,

            // Sync settings - not really used in vault-only mode but keep for compatibility
            syncMode = app.notedrop.android.domain.model.SyncMode.PUSH_ONLY,
            conflictStrategy = app.notedrop.android.domain.model.ConflictStrategy.LAST_WRITE_WINS,
            watchForChanges = false,
            autoSyncIntervalMinutes = 0,

            // Obsidian-specific features
            enableBacklinks = false,
            enableTemplateVariables = true
        )
    }

    /**
     * Create default config when vault config files don't exist
     */
    private fun createDefaultConfig(vaultUri: Uri): ProviderConfig.ObsidianConfig {
        return ProviderConfig.ObsidianConfig(
            vaultPath = vaultUri.toString(),
            dailyNotesPath = null, // Will use vault root
            dailyNotesFormat = "YYYY-MM-DD", // Standard Obsidian format
            templatePath = null,
            attachmentsPath = "attachments",
            useFrontMatter = true,
            preserveObsidianLinks = true,
            syncMode = app.notedrop.android.domain.model.SyncMode.PUSH_ONLY,
            conflictStrategy = app.notedrop.android.domain.model.ConflictStrategy.LAST_WRITE_WINS,
            watchForChanges = false,
            autoSyncIntervalMinutes = 0,
            enableBacklinks = false,
            enableTemplateVariables = true
        )
    }

    companion object {
        private const val TAG = "VaultConfigReader"
    }
}
