package app.notedrop.android.data.provider

import app.notedrop.android.data.parser.MarkdownParser
import app.notedrop.android.data.parser.ParserConfig
import app.notedrop.android.data.parser.SerializerConfig
import app.notedrop.android.data.provider.filesystem.FileSystemProvider
import app.notedrop.android.domain.model.FileMetadata
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.NoteMetadata
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.Vault
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider implementation for local file storage.
 * Simple folder with txt/md files - no cloud sync, just local organization.
 *
 * This provider uses the generic FileSystemProvider and MarkdownParser,
 * demonstrating how providers can reuse shared components.
 */
@Singleton
class LocalProvider @Inject constructor(
    private val fileSystemProvider: FileSystemProvider,
    private val markdownParser: MarkdownParser
) : NoteProvider {

    override suspend fun saveNote(note: Note, vault: Vault): Result<Unit> {
        return try {
            val config = vault.providerConfig as? ProviderConfig.LocalConfig
                ?: return Result.failure(IllegalArgumentException("Invalid Local config"))

            val file = getNoteFile(note, config)
            val serializerConfig = SerializerConfig(
                useFrontmatter = false, // Simple format for local files
                includeTitle = true,
                titleAsHeading = true
            )
            val content = markdownParser.serialize(note, serializerConfig)

            fileSystemProvider.writeFile(file.absolutePath, content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadNote(noteId: String, vault: Vault): Result<Note> {
        // TODO: Implement loading notes from local storage
        return Result.failure(NotImplementedError("Loading from local storage not yet implemented"))
    }

    override suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit> {
        // TODO: Implement deletion
        return Result.failure(NotImplementedError("Deletion not yet implemented"))
    }

    override suspend fun listNotes(vault: Vault): Result<List<NoteMetadata>> {
        // TODO: Implement listing
        return Result.failure(NotImplementedError("Listing not yet implemented"))
    }

    override suspend fun getMetadata(noteId: String, vault: Vault): Result<FileMetadata> {
        // TODO: Implement metadata retrieval
        return Result.failure(NotImplementedError("Metadata retrieval not yet implemented"))
    }

    override suspend fun isAvailable(vault: Vault): Boolean {
        val config = vault.providerConfig as? ProviderConfig.LocalConfig ?: return false
        val dir = File(config.storagePath)
        return dir.exists() && dir.isDirectory && dir.canWrite()
    }

    override fun getCapabilities(): ProviderCapabilities {
        return ProviderCapabilities(
            supportsVoiceRecordings = true,
            supportsImages = true,
            supportsAttachments = true,
            supportsTags = true,
            supportsMetadata = true,
            supportsFrontmatter = false,
            supportsEncryption = false,
            supportsBidirectionalSync = true,
            requiresInternet = false,
            isFileBased = true,
            isApiBased = false
        )
    }

    /**
     * Get the file for a note based on folder structure.
     */
    private fun getNoteFile(note: Note, config: ProviderConfig.LocalConfig): File {
        val baseDir = File(config.storagePath)

        val noteDir = when (config.folderStructure) {
            app.notedrop.android.domain.model.FolderStructure.FLAT -> baseDir

            app.notedrop.android.domain.model.FolderStructure.BY_DATE -> {
                val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")
                val datePath = dateFormatter.format(
                    note.createdAt.atZone(java.time.ZoneId.systemDefault())
                )
                File(baseDir, datePath)
            }

            app.notedrop.android.domain.model.FolderStructure.BY_YEAR_MONTH -> {
                val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM")
                val datePath = dateFormatter.format(
                    note.createdAt.atZone(java.time.ZoneId.systemDefault())
                )
                File(baseDir, datePath)
            }

            app.notedrop.android.domain.model.FolderStructure.BY_TAG -> {
                val tag = note.tags.firstOrNull() ?: "untagged"
                File(baseDir, tag)
            }
        }

        // Generate filename
        val filename = if (note.title != null && note.title.isNotBlank()) {
            fileSystemProvider.sanitizeFilename(note.title)
        } else {
            val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
            dateFormatter.format(note.createdAt.atZone(java.time.ZoneId.systemDefault()))
        }

        return File(noteDir, "$filename.${config.fileExtension}")
    }
}
