package app.notedrop.android.util

import app.notedrop.android.domain.model.*
import java.time.Instant
import java.util.UUID

/**
 * Test fixtures for creating sample data.
 */
object TestFixtures {

    /**
     * Create a sample Note.
     */
    fun createNote(
        id: String = UUID.randomUUID().toString(),
        content: String = "Test note content",
        title: String? = "Test Note",
        vaultId: String = "test-vault-id",
        tags: List<String> = listOf("test", "sample"),
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        voiceRecordingPath: String? = null,
        transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NONE,
        metadata: Map<String, String> = emptyMap(),
        isSynced: Boolean = false
    ) = Note(
        id = id,
        content = content,
        title = title,
        vaultId = vaultId,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        voiceRecordingPath = voiceRecordingPath,
        transcriptionStatus = transcriptionStatus,
        metadata = metadata,
        isSynced = isSynced
    )

    /**
     * Create a sample Vault.
     */
    fun createVault(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Vault",
        description: String? = "Test vault description",
        providerType: ProviderType = ProviderType.OBSIDIAN,
        providerConfig: ProviderConfig = createObsidianConfig(),
        isDefault: Boolean = false,
        isEncrypted: Boolean = false,
        createdAt: Instant = Instant.now(),
        lastSyncedAt: Instant? = null
    ) = Vault(
        id = id,
        name = name,
        description = description,
        providerType = providerType,
        providerConfig = providerConfig,
        isDefault = isDefault,
        isEncrypted = isEncrypted,
        createdAt = createdAt,
        lastSyncedAt = lastSyncedAt
    )

    /**
     * Create a sample Obsidian config.
     */
    fun createObsidianConfig(
        vaultPath: String = "/storage/emulated/0/TestVault",
        dailyNotesPath: String? = "daily",
        templatePath: String? = "templates",
        useFrontMatter: Boolean = true,
        frontMatterTemplate: String? = null
    ) = ProviderConfig.ObsidianConfig(
        vaultPath = vaultPath,
        dailyNotesPath = dailyNotesPath,
        templatePath = templatePath,
        useFrontMatter = useFrontMatter,
        frontMatterTemplate = frontMatterTemplate
    )

    /**
     * Create a sample Template.
     */
    fun createTemplate(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Template",
        content: String = "# {{title}}\n\n{{content}}",
        description: String? = "Test template description",
        variables: List<String> = listOf("title", "content"),
        isBuiltIn: Boolean = false,
        createdAt: Instant = Instant.now(),
        usageCount: Int = 0
    ) = Template(
        id = id,
        name = name,
        content = content,
        description = description,
        variables = variables,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt,
        usageCount = usageCount
    )

    /**
     * Create a list of sample notes.
     */
    fun createNotes(count: Int = 5): List<Note> {
        return (1..count).map { i ->
            createNote(
                content = "Test note content $i",
                title = "Test Note $i",
                tags = listOf("tag$i")
            )
        }
    }

    /**
     * Create a list of sample vaults.
     */
    fun createVaults(count: Int = 3): List<Vault> {
        return (1..count).map { i ->
            createVault(
                name = "Test Vault $i",
                isDefault = i == 1
            )
        }
    }

    /**
     * Create a list of sample templates.
     */
    fun createTemplates(count: Int = 3): List<Template> {
        return (1..count).map { i ->
            createTemplate(
                name = "Test Template $i",
                content = "Template content $i"
            )
        }
    }

    /**
     * Create a note with voice recording.
     */
    fun createVoiceNote(
        content: String = "Voice note content",
        voiceRecordingPath: String = "/storage/test/recording.m4a",
        transcriptionStatus: TranscriptionStatus = TranscriptionStatus.COMPLETED
    ) = createNote(
        content = content,
        voiceRecordingPath = voiceRecordingPath,
        transcriptionStatus = transcriptionStatus
    )

    /**
     * Create today's notes.
     */
    fun createTodaysNotes(count: Int = 3): List<Note> {
        val now = Instant.now()
        return (1..count).map { i ->
            createNote(
                content = "Today's note $i",
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
