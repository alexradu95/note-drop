package app.notedrop.android.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Tests for Vault domain model and ProviderConfig.
 */
class VaultTest {

    @Test
    fun `vault creation with obsidian provider`() {
        val config = ProviderConfig.ObsidianConfig(
            vaultPath = "/storage/vault",
            useFrontMatter = true
        )

        val vault = Vault(
            name = "My Vault",
            providerType = ProviderType.OBSIDIAN,
            providerConfig = config
        )

        assertThat(vault.id).isNotEmpty()
        assertThat(vault.name).isEqualTo("My Vault")
        assertThat(vault.providerType).isEqualTo(ProviderType.OBSIDIAN)
        assertThat(vault.isDefault).isFalse()
        assertThat(vault.isEncrypted).isFalse()
    }

    @Test
    fun `vault with description`() {
        val vault = Vault(
            name = "Vault",
            description = "Test description",
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/storage/local")
        )

        assertThat(vault.description).isEqualTo("Test description")
    }

    @Test
    fun `vault can be set as default`() {
        val vault = Vault(
            name = "Default Vault",
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/storage"),
            isDefault = true
        )

        assertThat(vault.isDefault).isTrue()
    }

    @Test
    fun `vault can be encrypted`() {
        val vault = Vault(
            name = "Encrypted Vault",
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/storage"),
            isEncrypted = true
        )

        assertThat(vault.isEncrypted).isTrue()
    }

    @Test
    fun `vault timestamps`() {
        val createdAt = Instant.now()
        val lastSyncedAt = Instant.now()

        val vault = Vault(
            name = "Vault",
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/storage"),
            createdAt = createdAt,
            lastSyncedAt = lastSyncedAt
        )

        assertThat(vault.createdAt).isEqualTo(createdAt)
        assertThat(vault.lastSyncedAt).isEqualTo(lastSyncedAt)
    }

    @Test
    fun `obsidian config with all options`() {
        val config = ProviderConfig.ObsidianConfig(
            vaultPath = "/vault",
            dailyNotesPath = "daily",
            templatePath = "templates",
            useFrontMatter = true,
            frontMatterTemplate = "custom: {{value}}"
        )

        assertThat(config.vaultPath).isEqualTo("/vault")
        assertThat(config.dailyNotesPath).isEqualTo("daily")
        assertThat(config.templatePath).isEqualTo("templates")
        assertThat(config.useFrontMatter).isTrue()
        assertThat(config.frontMatterTemplate).isEqualTo("custom: {{value}}")
    }

    @Test
    fun `obsidian config without optional fields`() {
        val config = ProviderConfig.ObsidianConfig(
            vaultPath = "/vault",
            useFrontMatter = false
        )

        assertThat(config.vaultPath).isEqualTo("/vault")
        assertThat(config.dailyNotesPath).isNull()
        assertThat(config.templatePath).isNull()
        assertThat(config.useFrontMatter).isFalse()
        assertThat(config.frontMatterTemplate).isNull()
    }

    @Test
    fun `local config simple path`() {
        val config = ProviderConfig.LocalConfig(
            storagePath = "/storage/notes"
        )

        assertThat(config.storagePath).isEqualTo("/storage/notes")
    }

    @Test
    fun `provider type enum values`() {
        assertThat(ProviderType.LOCAL).isNotNull()
        assertThat(ProviderType.OBSIDIAN).isNotNull()
    }

    @Test
    fun `vault copy preserves fields`() {
        val original = Vault(
            name = "Original",
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig("/vault"),
            isDefault = false
        )

        val updated = original.copy(isDefault = true)

        assertThat(updated.isDefault).isTrue()
        assertThat(updated.name).isEqualTo(original.name)
        assertThat(updated.providerType).isEqualTo(original.providerType)
        assertThat(updated.id).isEqualTo(original.id)
    }

    @Test
    fun `multiple vaults with different providers`() {
        val vault1 = Vault(
            name = "Obsidian Vault",
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig("/obsidian")
        )

        val vault2 = Vault(
            name = "Local Vault",
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/local")
        )

        assertThat(vault1.providerType).isNotEqualTo(vault2.providerType)
    }
}
