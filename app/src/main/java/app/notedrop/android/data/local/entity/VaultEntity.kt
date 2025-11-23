package app.notedrop.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.domain.model.Vault
import java.time.Instant

/**
 * Room entity for storing vaults in the local database.
 */
@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val providerType: String,
    val providerConfig: String, // Stored as JSON string
    val isDefault: Boolean,
    val isEncrypted: Boolean,
    val createdAt: Long,
    val lastSyncedAt: Long?
)

/**
 * Convert domain Vault to database VaultEntity.
 */
fun Vault.toEntity(): VaultEntity {
    return VaultEntity(
        id = id,
        name = name,
        description = description,
        providerType = providerType.name,
        providerConfig = providerConfigToJson(providerConfig),
        isDefault = isDefault,
        isEncrypted = isEncrypted,
        createdAt = createdAt.toEpochMilli(),
        lastSyncedAt = lastSyncedAt?.toEpochMilli()
    )
}

/**
 * Convert database VaultEntity to domain Vault.
 */
fun VaultEntity.toDomain(): Vault {
    return Vault(
        id = id,
        name = name,
        description = description,
        providerType = ProviderType.valueOf(providerType),
        providerConfig = jsonToProviderConfig(providerType, providerConfig),
        isDefault = isDefault,
        isEncrypted = isEncrypted,
        createdAt = Instant.ofEpochMilli(createdAt),
        lastSyncedAt = lastSyncedAt?.let { Instant.ofEpochMilli(it) }
    )
}

/**
 * Convert ProviderConfig to JSON string.
 * Simple implementation - will be enhanced with proper JSON library.
 */
private fun providerConfigToJson(config: ProviderConfig): String {
    return when (config) {
        is ProviderConfig.LocalConfig -> {
            """{"storagePath":"${config.storagePath}"}"""
        }
        is ProviderConfig.ObsidianConfig -> {
            """{"vaultPath":"${config.vaultPath}","dailyNotesPath":"${config.dailyNotesPath}","templatePath":"${config.templatePath}","useFrontMatter":${config.useFrontMatter}}"""
        }
        is ProviderConfig.NotionConfig -> {
            """{"workspaceId":"${config.workspaceId}","databaseId":"${config.databaseId}"}"""
        }
        is ProviderConfig.CustomConfig -> {
            "{}" // TODO: Implement proper serialization
        }
    }
}

/**
 * Convert JSON string to ProviderConfig based on provider type.
 * Simple implementation - will be enhanced with proper JSON library.
 */
private fun jsonToProviderConfig(type: String, json: String): ProviderConfig {
    // TODO: Implement proper JSON parsing
    return when (type) {
        "LOCAL" -> ProviderConfig.LocalConfig(storagePath = "")
        "OBSIDIAN" -> ProviderConfig.ObsidianConfig(vaultPath = "")
        "NOTION" -> ProviderConfig.NotionConfig(workspaceId = "")
        else -> ProviderConfig.CustomConfig(config = emptyMap())
    }
}
