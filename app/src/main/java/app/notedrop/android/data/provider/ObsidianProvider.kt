package app.notedrop.android.data.provider

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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

    override suspend fun saveNote(note: Note, vault: Vault): Result<String> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            android.util.Log.d("ObsidianProvider", "saveNote: vaultPath=${config.vaultPath}")

            // Get vault root as DocumentFile
            val vaultUri = Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                ?: return Result.failure(IllegalArgumentException("Cannot access vault at ${config.vaultPath}"))

            android.util.Log.d("ObsidianProvider", "Vault root: ${vaultRoot.name}, exists=${vaultRoot.exists()}")

            // Get or create the daily note file
            val dailyNoteFile = getDailyNoteFile(vaultRoot, config)
            android.util.Log.d("ObsidianProvider", "Daily note file: ${dailyNoteFile.name}, exists=${dailyNoteFile.exists()}")

            // Read existing content or create new
            val existingContent = if (dailyNoteFile.exists()) {
                context.contentResolver.openInputStream(dailyNoteFile.uri)?.use {
                    it.bufferedReader().readText()
                } ?: ""
            } else {
                ""
            }

            // Format the new note entry
            val noteEntry = formatNoteEntry(note, config)

            // Append to daily note under "## Drops" header
            val updatedContent = appendToDailyNote(existingContent, noteEntry)

            // Write the updated content
            context.contentResolver.openOutputStream(dailyNoteFile.uri, "wt")?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write(updatedContent)
                }
            }

            val relativePath = getDailyNoteRelativePath(config)
            android.util.Log.d("ObsidianProvider", "Note saved to: $relativePath")

            Result.success(relativePath)
        } catch (e: Exception) {
            android.util.Log.e("ObsidianProvider", "Error saving note", e)
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

    override suspend fun listNotes(vault: Vault): Result<List<app.notedrop.android.domain.model.NoteMetadata>> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            val vaultDir = File(config.vaultPath)
            if (!vaultDir.exists() || !vaultDir.isDirectory) {
                return Result.success(emptyList())
            }

            val notes = mutableListOf<app.notedrop.android.domain.model.NoteMetadata>()

            // Recursively find all markdown files
            vaultDir.walkTopDown()
                .filter { it.isFile && it.extension == "md" }
                .forEach { file ->
                    val relativePath = file.relativeTo(vaultDir).path
                    val title = file.nameWithoutExtension
                    notes.add(
                        app.notedrop.android.domain.model.NoteMetadata(
                            id = app.notedrop.android.domain.model.NoteMetadata.generateIdFromPath(relativePath),
                            title = title,
                            path = relativePath,
                            modifiedAt = java.time.Instant.ofEpochMilli(file.lastModified()),
                            size = file.length(),
                            tags = emptyList() // TODO: Parse tags from file content
                        )
                    )
                }

            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMetadata(noteId: String, vault: Vault): Result<app.notedrop.android.domain.model.FileMetadata> {
        // TODO: Implement metadata retrieval
        return Result.failure(NotImplementedError("Metadata retrieval not yet implemented"))
    }

    override suspend fun isAvailable(vault: Vault): Boolean {
        val config = vault.providerConfig as? ProviderConfig.ObsidianConfig ?: return false

        return try {
            val vaultUri = Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
            val isAvailable = vaultRoot != null && vaultRoot.exists() && vaultRoot.canWrite()

            android.util.Log.d("ObsidianProvider", "isAvailable: $isAvailable (vaultPath=${config.vaultPath})")
            isAvailable
        } catch (e: Exception) {
            android.util.Log.e("ObsidianProvider", "Error checking availability", e)
            false
        }
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
     * Get or create the daily note file for today
     */
    private fun getDailyNoteFile(vaultRoot: DocumentFile, config: ProviderConfig.ObsidianConfig): DocumentFile {
        // Parse the Obsidian format string if available, otherwise use default
        val (folderPath, filename) = if (!config.dailyNotesFormat.isNullOrBlank()) {
            parseDailyNotesFormat(config.dailyNotesPath, config.dailyNotesFormat)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = java.time.LocalDate.now().format(formatter)
            Pair(config.dailyNotesPath ?: "", "$today.md")
        }

        android.util.Log.d("ObsidianProvider", "Daily note path: folder='$folderPath', filename='$filename'")

        // Navigate to daily notes folder
        val targetFolder = if (folderPath.isNotBlank()) {
            findOrCreateFolder(vaultRoot, folderPath.trim('/'))
        } else {
            vaultRoot
        }

        // Find or create the daily note file
        return targetFolder.findFile(filename) ?: targetFolder.createFile("text/markdown", filename)!!
    }

    /**
     * Parse Obsidian daily notes format string and generate folder path + filename
     * Format example: "YYYY/MM-MMMM/[Week-]WW/YYYY-MM-DD"
     * Result: ("2025/11-November/Week-48", "2025-11-24.md")
     *
     * Supports moment.js tokens used by Obsidian:
     * - YYYY: 4-digit year
     * - YY: 2-digit year
     * - MMMM: Full month name (November)
     * - MMM: Short month name (Nov)
     * - MM: 2-digit month (11)
     * - M: Month without leading zero (11)
     * - DD: 2-digit day (24)
     * - D: Day without leading zero (24)
     * - WW: ISO week number with leading zero (48)
     * - W: ISO week number without leading zero (48)
     * - [...]: Literal text
     */
    private fun parseDailyNotesFormat(baseFolder: String?, format: String): Pair<String, String> {
        val now = java.time.LocalDate.now()
        val year = now.year
        val month = now.monthValue
        val day = now.dayOfMonth
        val weekOfYear = now.get(java.time.temporal.WeekFields.ISO.weekOfYear())

        // Get month names
        val fullMonthName = now.month.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.ENGLISH
        )
        val shortMonthName = now.month.getDisplayName(
            java.time.format.TextStyle.SHORT,
            java.util.Locale.ENGLISH
        )

        // Handle literal text in brackets [...] first
        val literalRegex = """\[([^\]]+)\]""".toRegex()
        var result = literalRegex.replace(format) { matchResult ->
            // Use a placeholder that won't conflict with date tokens
            "\u0000${matchResult.groupValues[1]}\u0000"
        }

        // Token replacements - IMPORTANT: Order matters! Longer tokens first
        val tokens = listOf(
            "YYYY" to year.toString(),
            "YY" to (year % 100).toString().padStart(2, '0'),
            "MMMM" to fullMonthName,
            "MMM" to shortMonthName,
            "MM" to month.toString().padStart(2, '0'),
            "M" to month.toString(),
            "DD" to day.toString().padStart(2, '0'),
            "D" to day.toString(),
            "WW" to weekOfYear.toString().padStart(2, '0'),
            "W" to weekOfYear.toString()
        )

        // Apply replacements
        for ((token, value) in tokens) {
            result = result.replace(token, value)
        }

        // Restore literal text (remove placeholders)
        result = result.replace("\u0000", "")

        // Split into path components - last component is the filename
        val components = result.split("/")
        val filename = if (components.isNotEmpty()) {
            "${components.last()}.md"
        } else {
            "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}.md"
        }

        // Build folder path (everything except the last component)
        val folderComponents = components.dropLast(1)
        val folderPath = if (baseFolder.isNullOrBlank()) {
            folderComponents.joinToString("/")
        } else {
            (listOf(baseFolder.trim('/')) + folderComponents).filter { it.isNotBlank() }.joinToString("/")
        }

        return Pair(folderPath, filename)
    }

    /**
     * Find or create a folder path (handles nested folders)
     */
    private fun findOrCreateFolder(root: DocumentFile, path: String): DocumentFile {
        if (path.isBlank()) return root

        var current = root
        path.split('/').forEach { folderName ->
            if (folderName.isNotBlank()) {
                current = current.findFile(folderName) ?: current.createDirectory(folderName)!!
            }
        }
        return current
    }

    /**
     * Get the relative path for today's daily note
     */
    private fun getDailyNoteRelativePath(config: ProviderConfig.ObsidianConfig): String {
        val (folderPath, filename) = if (!config.dailyNotesFormat.isNullOrBlank()) {
            parseDailyNotesFormat(config.dailyNotesPath, config.dailyNotesFormat)
        } else {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val today = java.time.LocalDate.now().format(formatter)
            Pair(config.dailyNotesPath ?: "", "$today.md")
        }

        return if (folderPath.isNotBlank()) {
            "$folderPath/$filename"
        } else {
            filename
        }
    }

    /**
     * Format a single note entry (without front matter, just the content)
     */
    private fun formatNoteEntry(note: Note, config: ProviderConfig.ObsidianConfig): String {
        val builder = StringBuilder()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val time = note.createdAt.atZone(ZoneId.systemDefault()).format(timeFormatter)

        // Add timestamp as bullet point
        builder.append("- **$time** ")

        // Add content
        builder.append(note.content)

        // Add tags inline if present
        if (note.tags.isNotEmpty()) {
            builder.append(" ")
            builder.append(note.tags.joinToString(" ") { "#$it" })
        }

        builder.append("\n")

        return builder.toString()
    }

    /**
     * Append a note entry to the daily note under "## Drops" header
     */
    private fun appendToDailyNote(existingContent: String, noteEntry: String): String {
        val dropsHeader = "## Drops"

        return when {
            // File is empty - create new with Drops header
            existingContent.isBlank() -> {
                "$dropsHeader\n\n$noteEntry"
            }
            // File already has Drops header - append after it
            existingContent.contains(dropsHeader) -> {
                // Find the Drops section and append
                val lines = existingContent.lines().toMutableList()
                val dropsIndex = lines.indexOfFirst { it.trim() == dropsHeader }

                if (dropsIndex != -1) {
                    // Find where to insert (after the header and any existing content under it)
                    var insertIndex = dropsIndex + 1

                    // Skip empty lines after header
                    while (insertIndex < lines.size && lines[insertIndex].isBlank()) {
                        insertIndex++
                    }

                    // Skip existing drop entries
                    while (insertIndex < lines.size &&
                           (lines[insertIndex].trim().startsWith("-") || lines[insertIndex].isBlank())) {
                        insertIndex++
                    }

                    // Insert before the next section or at the end of drops
                    lines.add(insertIndex, noteEntry.trimEnd())
                    lines.joinToString("\n")
                } else {
                    // Shouldn't happen, but fallback
                    "$existingContent\n\n$dropsHeader\n\n$noteEntry"
                }
            }
            // File doesn't have Drops header - append at the end
            else -> {
                "$existingContent\n\n$dropsHeader\n\n$noteEntry"
            }
        }
    }
}
