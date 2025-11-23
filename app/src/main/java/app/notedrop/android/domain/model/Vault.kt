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
    LOCAL,      // Local device storage
    OBSIDIAN,   // Obsidian vault
    NOTION,     // Notion workspace
    CUSTOM      // Custom provider (future extensibility)
}

/**
 * Base configuration for vault providers.
 */
sealed class ProviderConfig {
    /**
     * Configuration for local storage provider.
     */
    data class LocalConfig(
        val storagePath: String
    ) : ProviderConfig()

    /**
     * Configuration for Obsidian vault provider.
     */
    data class ObsidianConfig(
        val vaultPath: String,
        val dailyNotesPath: String? = null,
        val templatePath: String? = null,
        val useFrontMatter: Boolean = true,
        val frontMatterTemplate: String? = null
    ) : ProviderConfig()

    /**
     * Configuration for Notion provider (future).
     */
    data class NotionConfig(
        val workspaceId: String,
        val databaseId: String? = null,
        val apiKey: String? = null
    ) : ProviderConfig()

    /**
     * Configuration for custom providers (future extensibility).
     */
    data class CustomConfig(
        val config: Map<String, String>
    ) : ProviderConfig()
}
