package app.notedrop.android.data.provider.filesystem

import android.content.Context
import android.os.FileObserver
import app.notedrop.android.domain.model.FileEvent
import app.notedrop.android.domain.model.FileMetadata
import app.notedrop.android.domain.model.FilePattern
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of FileSystemProvider.
 * Handles all file system operations for file-based providers.
 */
@Singleton
class AndroidFileSystemProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : FileSystemProvider {

    private val fileObservers = mutableMapOf<String, FileObserver>()

    override suspend fun readFile(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $path"))
            }
            Result.success(file.readText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeFile(path: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()

            // Atomic write: write to temp file, then rename
            val tempFile = File(file.parentFile, ".${file.name}.tmp")
            try {
                tempFile.writeText(content)

                // Atomic rename
                if (!tempFile.renameTo(file)) {
                    // Fallback: direct write if rename fails
                    file.writeText(content)
                }

                Result.success(Unit)
            } finally {
                // Clean up temp file if it still exists
                tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists() && file.delete()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete file: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fileExists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun listFiles(
        directory: String,
        pattern: FilePattern
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(directory)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext Result.failure(Exception("Directory not found: $directory"))
            }

            val files = mutableListOf<String>()
            collectFiles(dir, dir, pattern, files, 0)

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectFiles(
        baseDir: File,
        currentDir: File,
        pattern: FilePattern,
        result: MutableList<String>,
        depth: Int
    ) {
        // Check max depth
        if (pattern.maxDepth != null && depth > pattern.maxDepth) {
            return
        }

        currentDir.listFiles()?.forEach { file ->
            val relativePath = file.relativeTo(baseDir).path

            // Skip excluded patterns
            if (pattern.excludePatterns.any { relativePath.contains(it, ignoreCase = true) }) {
                return@forEach
            }

            if (file.isDirectory) {
                if (pattern.includeSubdirectories) {
                    collectFiles(baseDir, file, pattern, result, depth + 1)
                }
            } else if (pattern.matches(relativePath)) {
                result.add(file.absolutePath)
            }
        }
    }

    override suspend fun getMetadata(path: String): Result<FileMetadata> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $path"))
            }

            Result.success(
                FileMetadata(
                    path = file.name,
                    absolutePath = file.absolutePath,
                    modifiedAt = Instant.ofEpochMilli(file.lastModified()),
                    size = file.length(),
                    checksum = null, // Checksum is expensive, calculate only when needed
                    exists = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun watchDirectory(
        directory: String,
        pattern: FilePattern,
        callback: (FileEvent) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val dir = File(directory)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext
            }

            // Stop existing observer for this directory
            stopWatching(directory)

            // Create new observer
            val observer = object : FileObserver(
                dir,
                CREATE or MODIFY or DELETE or MOVED_FROM or MOVED_TO
            ) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return

                    // Check if file matches pattern
                    if (!pattern.matches(path)) return

                    val fullPath = File(directory, path).absolutePath
                    val fileEvent = when (event and ALL_EVENTS) {
                        CREATE -> FileEvent.Created(fullPath)
                        MODIFY -> FileEvent.Modified(fullPath)
                        DELETE -> FileEvent.Deleted(fullPath)
                        MOVED_FROM, MOVED_TO -> FileEvent.Modified(fullPath)
                        else -> return
                    }

                    callback(fileEvent)
                }
            }

            observer.startWatching()
            fileObservers[directory] = observer
        }
    }

    override suspend fun stopWatching(directory: String) {
        withContext(Dispatchers.IO) {
            fileObservers[directory]?.let { observer ->
                observer.stopWatching()
                fileObservers.remove(directory)
            }
        }
    }

    override suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)
            if (dir.exists()) {
                Result.success(Unit)
            } else if (dir.mkdirs()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create directory: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copyFile(sourcePath: String, destPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)

            if (!source.exists()) {
                return@withContext Result.failure(Exception("Source file not found: $sourcePath"))
            }

            dest.parentFile?.mkdirs()
            source.copyTo(dest, overwrite = true)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveFile(sourcePath: String, destPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destPath)

            if (!source.exists()) {
                return@withContext Result.failure(Exception("Source file not found: $sourcePath"))
            }

            dest.parentFile?.mkdirs()

            if (source.renameTo(dest)) {
                Result.success(Unit)
            } else {
                // Fallback: copy then delete
                source.copyTo(dest, overwrite = true)
                source.delete()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateChecksum(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $path"))
            }

            val digest = MessageDigest.getInstance("MD5")
            val bytes = file.readBytes()
            val hash = digest.digest(bytes)

            Result.success(hash.joinToString("") { "%02x".format(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileSize(path: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $path"))
            }
            Result.success(file.length())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean up all file observers when provider is destroyed.
     */
    fun cleanup() {
        fileObservers.values.forEach { it.stopWatching() }
        fileObservers.clear()
    }
}
