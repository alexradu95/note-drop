package app.notedrop.android.data.provider

import android.content.Context
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.Vault
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider implementation for Obsidian vaults.
 * Saves notes as Markdown files with optional front-matter.
 */
@Singleton
class ObsidianProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : NoteProvider {

    override suspend fun saveNote(note: Note, vault: Vault): Result<Unit> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            val file = getNoteFile(note, config)
            val content = formatNoteAsMarkdown(note, config)

            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()

            // Write the note content
            file.writeText(content)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadNote(noteId: String, vault: Vault): Result<Note> {
        // TODO: Implement loading notes from Obsidian vault
        return Result.failure(NotImplementedError("Loading from Obsidian not yet implemented"))
    }

    override suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            // TODO: Implement note deletion
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAvailable(vault: Vault): Boolean {
        val config = vault.providerConfig as? ProviderConfig.ObsidianConfig ?: return false
        val vaultDir = File(config.vaultPath)
        return vaultDir.exists() && vaultDir.isDirectory && vaultDir.canWrite()
    }

    override fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            supportsVoiceRecordings = true,
            supportsImages = true,
            supportsTags = true,
            supportsMetadata = true,
            supportsEncryption = false,
            requiresInternet = false
        )
    }

    /**
     * Get the file for a note.
     */
    private fun getNoteFile(note: Note, config: ProviderConfig.ObsidianConfig): File {
        val vaultDir = File(config.vaultPath)

        // Use daily notes path if configured, otherwise root
        val noteDir = if (config.dailyNotesPath != null) {
            File(vaultDir, config.dailyNotesPath)
        } else {
            vaultDir
        }

        // Generate filename from title or timestamp
        val filename = generateFilename(note)

        return File(noteDir, "$filename.md")
    }

    /**
     * Generate a filename for the note.
     */
    private fun generateFilename(note: Note): String {
        return if (note.title != null && note.title.isNotBlank()) {
            // Sanitize title for filename
            note.title
                .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
                .take(50)
        } else {
            // Use timestamp
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
            note.createdAt.atZone(ZoneId.systemDefault()).format(formatter)
        }
    }

    /**
     * Format a note as Markdown with optional front-matter.
     */
    private fun formatNoteAsMarkdown(note: Note, config: ProviderConfig.ObsidianConfig): String {
        val builder = StringBuilder()

        // Add front-matter if enabled
        if (config.useFrontMatter) {
            builder.append("---\n")
            builder.append("created: ${note.createdAt}\n")
            builder.append("updated: ${note.updatedAt}\n")

            if (note.tags.isNotEmpty()) {
                builder.append("tags:\n")
                note.tags.forEach { tag ->
                    builder.append("  - $tag\n")
                }
            }

            // Add custom front-matter from template if provided
            config.frontMatterTemplate?.let { template ->
                builder.append(template)
                builder.append("\n")
            }

            builder.append("---\n\n")
        }

        // Add title if present
        if (note.title != null && note.title.isNotBlank()) {
            builder.append("# ${note.title}\n\n")
        }

        // Add content
        builder.append(note.content)

        // Add tags inline if not using front-matter
        if (!config.useFrontMatter && note.tags.isNotEmpty()) {
            builder.append("\n\n")
            builder.append(note.tags.joinToString(" ") { "#$it" })
        }

        // Add voice recording reference if present
        if (note.voiceRecordingPath != null) {
            builder.append("\n\n---\n")
            builder.append("Voice Recording: `${note.voiceRecordingPath}`\n")
        }

        return builder.toString()
    }

    /**
     * Create a daily note path for today.
     */
    fun getDailyNotePath(config: ProviderConfig.ObsidianConfig): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = java.time.LocalDate.now().format(formatter)

        val dailyNotesPath = config.dailyNotesPath ?: ""
        return "$dailyNotesPath/$today.md"
    }
}
