package app.notedrop.android.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model representing a vault (storage location for notes).
 *
 * @property id Unique identifier for the vault
 * @property name User-friendly name of the vault
 * @property description Optional description
 * @property providerType Type of provider (Obsidian, Local, Notion, etc.)
 * @property providerConfig Configuration specific to the provider
 * @property isDefault Whether this is the default vault
 * @property isEncrypted Whether notes in this vault are encrypted
 * @property createdAt Timestamp when the vault was created
 * @property lastSyncedAt Timestamp of last successful sync
 */
data class Vault(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val providerType: ProviderType,
    val providerConfig: ProviderConfig,
    val isDefault: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val lastSyncedAt: Instant? = null
)

/**
 * Types of storage providers supported.
 */
enum class ProviderType {
    LOCAL,      // Local device storage (simple folder with txt/md files)
    OBSIDIAN    // Obsidian vault
}

/**
 * Base configuration for vault providers.
 */
sealed class ProviderConfig {
    /**
     * Configuration for local file-based storage provider.
     * Simple folder structure with txt/md files - no cloud sync, just local organization.
     */
    data class LocalConfig(
        val storagePath: String,
        val fileExtension: String = "md",
        val useSubfolders: Boolean = true,
        val folderStructure: FolderStructure = FolderStructure.FLAT
    ) : ProviderConfig()

    /**
     * Configuration for Obsidian vault provider.
     * Supports full Obsidian features: frontmatter, wiki-links, daily notes, templates.
     */
    data class ObsidianConfig(
        // Storage paths
        val vaultPath: String,
        val dailyNotesPath: String? = null,
        val dailyNotesFormat: String? = null, // Obsidian date format (e.g., "YYYY/MM-MMMM/[Week-]WW/YYYY-MM-DD")
        val templatePath: String? = null,
        val attachmentsPath: String? = "attachments",

        // Markdown format settings
        val useFrontMatter: Boolean = true,
        val frontMatterTemplate: String? = null,
        val preserveObsidianLinks: Boolean = true,

        // Sync settings
        val syncMode: SyncMode = SyncMode.BIDIRECTIONAL,
        val conflictStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS,
        val watchForChanges: Boolean = true,
        val autoSyncIntervalMinutes: Int = 30,

        // Obsidian-specific features
        val enableBacklinks: Boolean = false,
        val enableTemplateVariables: Boolean = true
    ) : ProviderConfig()

}

/**
 * Folder structure for local file-based providers.
 */
enum class FolderStructure {
    /**
     * All files in one folder.
     */
    FLAT,

    /**
     * Organize by date (YYYY/MM/DD/).
     */
    BY_DATE,

    /**
     * Organize by first tag.
     */
    BY_TAG,

    /**
     * Organize by year and month (YYYY/MM/).
     */
    BY_YEAR_MONTH
}
