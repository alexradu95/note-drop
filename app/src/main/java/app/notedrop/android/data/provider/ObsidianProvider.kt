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
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            android.util.Log.d("ObsidianProvider", "loadNote: noteId=$noteId")

            // Get vault root as DocumentFile
            val vaultUri = Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                ?: return Result.failure(IllegalArgumentException("Cannot access vault at ${config.vaultPath}"))

            // Convert noteId to path (noteId is relative path without extension)
            val relativePath = noteId.replace("_", "/") + ".md"

            // Navigate to the file
            val file = findFileByPath(vaultRoot, relativePath)
                ?: return Result.failure(IllegalArgumentException("Note not found: $relativePath"))

            // Read file content
            val content = context.contentResolver.openInputStream(file.uri)?.use {
                it.bufferedReader().readText()
            } ?: return Result.failure(IllegalArgumentException("Cannot read note content"))

            // Parse the markdown content
            val parsedNote = parseMarkdownContent(content, noteId, vault.id, relativePath)

            android.util.Log.d("ObsidianProvider", "Note loaded: $relativePath")
            Result.success(parsedNote)
        } catch (e: Exception) {
            android.util.Log.e("ObsidianProvider", "Error loading note", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            android.util.Log.d("ObsidianProvider", "deleteNote: noteId=$noteId")

            // Get vault root as DocumentFile
            val vaultUri = Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                ?: return Result.failure(IllegalArgumentException("Cannot access vault at ${config.vaultPath}"))

            // Convert noteId to path (noteId is relative path without extension)
            val relativePath = noteId.replace("_", "/") + ".md"

            // Navigate to the file
            val file = findFileByPath(vaultRoot, relativePath)
                ?: return Result.failure(IllegalArgumentException("Note not found: $relativePath"))

            // Check if this is a daily note by checking if it matches the daily note pattern
            val isDailyNote = isDailyNoteFile(relativePath, config)

            if (isDailyNote) {
                // For daily notes, remove the entry from "## Drops" section, don't delete the file
                android.util.Log.d("ObsidianProvider", "Deleting entry from daily note: $relativePath")

                // Read existing content
                val content = context.contentResolver.openInputStream(file.uri)?.use {
                    it.bufferedReader().readText()
                } ?: return Result.failure(IllegalArgumentException("Cannot read note content"))

                // Remove the entry by timestamp (noteId contains timestamp info)
                val updatedContent = removeEntryFromDailyNote(content, noteId)

                // Write back the updated content
                context.contentResolver.openOutputStream(file.uri, "wt")?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.write(updatedContent)
                    }
                }

                android.util.Log.d("ObsidianProvider", "Entry removed from daily note")
            } else {
                // For standalone notes, delete the entire file
                android.util.Log.d("ObsidianProvider", "Deleting standalone note file: $relativePath")
                val deleted = file.delete()
                if (!deleted) {
                    return Result.failure(IllegalStateException("Failed to delete file: $relativePath"))
                }
                android.util.Log.d("ObsidianProvider", "Note file deleted")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ObsidianProvider", "Error deleting note", e)
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
        return try {
            val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Obsidian config"))

            android.util.Log.d("ObsidianProvider", "getMetadata: noteId=$noteId")

            // Get vault root as DocumentFile
            val vaultUri = Uri.parse(config.vaultPath)
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                ?: return Result.failure(IllegalArgumentException("Cannot access vault at ${config.vaultPath}"))

            // Convert noteId to path (noteId is relative path without extension)
            val relativePath = noteId.replace("_", "/") + ".md"

            // Navigate to the file
            val file = findFileByPath(vaultRoot, relativePath)
                ?: return Result.failure(IllegalArgumentException("Note not found: $relativePath"))

            // Get file metadata
            val modifiedAt = java.time.Instant.ofEpochMilli(file.lastModified())
            val size = file.length()

            // Build absolute path (use URI as absolute path for DocumentFile)
            val absolutePath = file.uri.toString()

            val metadata = app.notedrop.android.domain.model.FileMetadata(
                path = relativePath,
                absolutePath = absolutePath,
                modifiedAt = modifiedAt,
                size = size,
                checksum = null, // Checksum calculation would require reading file content
                exists = file.exists()
            )

            android.util.Log.d("ObsidianProvider", "Metadata retrieved: $relativePath")
            Result.success(metadata)
        } catch (e: Exception) {
            android.util.Log.e("ObsidianProvider", "Error getting metadata", e)
            Result.failure(e)
        }
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

        // Apply replacements - ONLY outside of literal markers
        for ((token, value) in tokens) {
            result = replaceTokenOutsideLiterals(result, token, value)
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
     * Replace a token with its value, but only outside of literal text markers (\u0000...\u0000).
     * This prevents tokens inside literal brackets from being replaced.
     *
     * Example:
     * - Input: "\u0000Week-\u0000WW" with token="W", value="48"
     * - Output: "\u0000Week-\u000048" (only the second W is replaced)
     */
    private fun replaceTokenOutsideLiterals(text: String, token: String, value: String): String {
        val result = StringBuilder()
        var i = 0
        var insideLiteral = false

        while (i < text.length) {
            when {
                // Toggle literal marker
                text[i] == '\u0000' -> {
                    insideLiteral = !insideLiteral
                    result.append(text[i])
                    i++
                }
                // Found token outside literal - replace it
                !insideLiteral && text.startsWith(token, i) -> {
                    result.append(value)
                    i += token.length
                }
                // Regular character or token inside literal - keep as-is
                else -> {
                    result.append(text[i])
                    i++
                }
            }
        }

        return result.toString()
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

    /**
     * Find a file by its relative path from vault root
     */
    private fun findFileByPath(root: DocumentFile, relativePath: String): DocumentFile? {
        var current = root
        val parts = relativePath.split('/')

        // Navigate through folders
        for (i in 0 until parts.size - 1) {
            current = current.findFile(parts[i]) ?: return null
        }

        // Find the final file
        return current.findFile(parts.last())
    }

    /**
     * Parse markdown content and extract Note model
     */
    private fun parseMarkdownContent(content: String, noteId: String, vaultId: String, filePath: String): Note {
        val lines = content.lines()
        var currentIndex = 0

        // Parse front-matter if present
        val frontMatter = mutableMapOf<String, String>()
        val tags = mutableListOf<String>()
        var createdAt: java.time.Instant? = null
        var updatedAt: java.time.Instant? = null

        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            // Parse YAML front-matter
            currentIndex = 1
            while (currentIndex < lines.size && lines[currentIndex].trim() != "---") {
                val line = lines[currentIndex]
                if (line.contains(":")) {
                    val (key, value) = line.split(":", limit = 2)
                    val trimmedKey = key.trim()
                    val trimmedValue = value.trim()

                    frontMatter[trimmedKey] = trimmedValue

                    // Parse specific fields
                    when (trimmedKey) {
                        "created" -> createdAt = tryParseInstant(trimmedValue)
                        "updated" -> updatedAt = tryParseInstant(trimmedValue)
                        "tags" -> {
                            // Start of tags array
                            currentIndex++
                            while (currentIndex < lines.size && lines[currentIndex].trim().startsWith("-")) {
                                val tag = lines[currentIndex].trim().removePrefix("-").trim()
                                if (tag.isNotBlank()) tags.add(tag)
                                currentIndex++
                            }
                            currentIndex-- // Back up one since we'll increment at loop end
                        }
                    }
                }
                currentIndex++
            }
            if (currentIndex < lines.size) currentIndex++ // Skip closing ---
        }

        // Skip empty lines after front-matter
        while (currentIndex < lines.size && lines[currentIndex].isBlank()) {
            currentIndex++
        }

        // Extract title if present (starts with #)
        var title: String? = null
        if (currentIndex < lines.size && lines[currentIndex].trim().startsWith("#")) {
            title = lines[currentIndex].trim().removePrefix("#").trim()
            currentIndex++
            // Skip empty lines after title
            while (currentIndex < lines.size && lines[currentIndex].isBlank()) {
                currentIndex++
            }
        }

        // Rest is content
        val contentLines = lines.subList(currentIndex, lines.size)
        val noteContent = contentLines.joinToString("\n").trim()

        // Extract inline tags from content if no front-matter tags
        if (tags.isEmpty()) {
            val inlineTagRegex = """#(\w+)""".toRegex()
            tags.addAll(inlineTagRegex.findAll(noteContent).map { it.groupValues[1] })
        }

        return Note(
            id = noteId,
            content = noteContent,
            title = title,
            vaultId = vaultId,
            tags = tags.distinct(),
            createdAt = createdAt ?: java.time.Instant.now(),
            updatedAt = updatedAt ?: java.time.Instant.now(),
            metadata = frontMatter,
            isSynced = true,
            filePath = filePath
        )
    }

    /**
     * Try to parse an instant from string (supports ISO-8601 format)
     */
    private fun tryParseInstant(value: String): java.time.Instant? {
        return try {
            java.time.Instant.parse(value)
        } catch (e: Exception) {
            try {
                // Try parsing as LocalDateTime and convert
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                java.time.LocalDateTime.parse(value, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            } catch (e2: Exception) {
                android.util.Log.w("ObsidianProvider", "Failed to parse timestamp: $value", e2)
                null
            }
        }
    }

    /**
     * Check if a file path matches the daily note pattern
     */
    private fun isDailyNoteFile(relativePath: String, config: ProviderConfig.ObsidianConfig): Boolean {
        val expectedPath = getDailyNoteRelativePath(config)
        return relativePath == expectedPath || relativePath.startsWith((config.dailyNotesPath ?: "") + "/")
    }

    /**
     * Remove a note entry from daily note by timestamp
     * Daily note entries are in format: "- **HH:mm** content #tags"
     */
    private fun removeEntryFromDailyNote(content: String, noteId: String): String {
        // For daily notes, we need to identify and remove the specific entry
        // The noteId might contain timestamp information we can use

        val lines = content.lines().toMutableList()
        val dropsHeader = "## Drops"
        val dropsIndex = lines.indexOfFirst { it.trim() == dropsHeader }

        if (dropsIndex == -1) {
            // No Drops section, return content as-is
            return content
        }

        // Find entries in the Drops section
        var currentIndex = dropsIndex + 1
        val entriesToRemove = mutableListOf<Int>()

        // Skip empty lines after header
        while (currentIndex < lines.size && lines[currentIndex].isBlank()) {
            currentIndex++
        }

        // Find matching entries (we'll remove the first one if noteId doesn't have specific info)
        // In a real implementation, you'd want to track entry IDs more precisely
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]

            // Check if this is still part of Drops section
            if (line.trim().startsWith("##")) {
                // Hit next section
                break
            }

            // Check if this is an entry line (starts with "- **")
            if (line.trim().startsWith("- **")) {
                // For now, we'll mark entries for potential removal
                // In a more sophisticated implementation, you'd match based on content hash or ID
                entriesToRemove.add(currentIndex)
            }

            currentIndex++
        }

        // Remove the first entry (or the one matching the noteId if we had more info)
        // This is a simplified approach - ideally you'd store entry IDs
        if (entriesToRemove.isNotEmpty()) {
            lines.removeAt(entriesToRemove[0])
        }

        return lines.joinToString("\n")
    }
}
