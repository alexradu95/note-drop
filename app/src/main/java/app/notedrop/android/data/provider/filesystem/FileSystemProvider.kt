package app.notedrop.android.data.provider.filesystem

import app.notedrop.android.domain.model.FileEvent
import app.notedrop.android.domain.model.FileMetadata
import app.notedrop.android.domain.model.FilePattern
import java.io.File

/**
 * Generic file system operations for file-based providers.
 * This abstraction allows different providers (Obsidian, Local, Logseq, etc.)
 * to share the same file handling logic.
 *
 * This is provider-agnostic - it only deals with files, not note formats.
 */
interface FileSystemProvider {

    /**
     * Read file contents as string.
     * @param path Absolute path to file
     * @return File contents or error
     */
    suspend fun readFile(path: String): Result<String>

    /**
     * Write content to file atomically.
     * Uses temp file + rename to ensure atomic writes.
     * @param path Absolute path to file
     * @param content Content to write
     * @return Success or error
     */
    suspend fun writeFile(path: String, content: String): Result<Unit>

    /**
     * Delete a file.
     * @param path Absolute path to file
     * @return Success or error
     */
    suspend fun deleteFile(path: String): Result<Unit>

    /**
     * Check if file exists.
     * @param path Absolute path to file
     * @return true if file exists
     */
    suspend fun fileExists(path: String): Boolean

    /**
     * List all files in a directory matching a pattern.
     * @param directory Absolute path to directory
     * @param pattern File pattern to match (extension, depth, etc.)
     * @return List of absolute file paths or error
     */
    suspend fun listFiles(
        directory: String,
        pattern: FilePattern = FilePattern()
    ): Result<List<String>>

    /**
     * Get metadata for a file.
     * @param path Absolute path to file
     * @return File metadata or error
     */
    suspend fun getMetadata(path: String): Result<FileMetadata>

    /**
     * Watch a directory for file changes.
     * @param directory Absolute path to directory
     * @param pattern File pattern to watch
     * @param callback Called when a file event occurs
     */
    suspend fun watchDirectory(
        directory: String,
        pattern: FilePattern = FilePattern(),
        callback: (FileEvent) -> Unit
    )

    /**
     * Stop watching a directory.
     * @param directory Absolute path to directory
     */
    suspend fun stopWatching(directory: String)

    /**
     * Create directory if it doesn't exist.
     * @param path Absolute path to directory
     * @return Success or error
     */
    suspend fun createDirectory(path: String): Result<Unit>

    /**
     * Copy a file from source to destination.
     * @param sourcePath Absolute path to source file
     * @param destPath Absolute path to destination file
     * @return Success or error
     */
    suspend fun copyFile(sourcePath: String, destPath: String): Result<Unit>

    /**
     * Move/rename a file.
     * @param sourcePath Absolute path to source file
     * @param destPath Absolute path to destination file
     * @return Success or error
     */
    suspend fun moveFile(sourcePath: String, destPath: String): Result<Unit>

    /**
     * Calculate checksum (MD5) for a file.
     * Used for change detection.
     * @param path Absolute path to file
     * @return Checksum string or error
     */
    suspend fun calculateChecksum(path: String): Result<String>

    /**
     * Get file size in bytes.
     * @param path Absolute path to file
     * @return File size or error
     */
    suspend fun getFileSize(path: String): Result<Long>

    /**
     * Resolve a relative path to absolute path.
     * @param basePath Base directory path
     * @param relativePath Relative path from base
     * @return Absolute path
     */
    fun resolvePath(basePath: String, relativePath: String): String {
        return File(basePath, relativePath).absolutePath
    }

    /**
     * Get relative path from base directory.
     * @param basePath Base directory path
     * @param absolutePath Absolute file path
     * @return Relative path or null if not under base
     */
    fun getRelativePath(basePath: String, absolutePath: String): String? {
        val base = File(basePath).canonicalFile
        val file = File(absolutePath).canonicalFile
        return if (file.startsWith(base)) {
            file.relativeTo(base).path
        } else {
            null
        }
    }

    /**
     * Sanitize filename to be safe for file system.
     * Removes invalid characters.
     * @param filename Original filename
     * @return Sanitized filename
     */
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), "-")
            .take(255) // Max filename length
    }
}
