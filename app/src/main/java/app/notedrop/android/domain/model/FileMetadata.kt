package app.notedrop.android.domain.model

import java.time.Instant

/**
 * Metadata about a file in a provider's storage.
 * Generic model that works with any file-based provider (Obsidian, simple folder, etc.).
 *
 * @property path Relative path from vault root (e.g., "daily/2024-01-15.md")
 * @property absolutePath Full system path to the file
 * @property modifiedAt Last modification timestamp
 * @property size File size in bytes
 * @property checksum Optional checksum (MD5/SHA) for change detection
 * @property exists Whether the file currently exists
 */
data class FileMetadata(
    val path: String,
    val absolutePath: String,
    val modifiedAt: Instant,
    val size: Long,
    val checksum: String? = null,
    val exists: Boolean = true
) {
    /**
     * Check if this file has been modified after a given timestamp.
     */
    fun isModifiedAfter(timestamp: Instant): Boolean {
        return modifiedAt.isAfter(timestamp)
    }

    /**
     * Check if this file has a different checksum (content changed).
     */
    fun hasChangedContent(otherChecksum: String?): Boolean {
        if (checksum == null || otherChecksum == null) return false
        return checksum != otherChecksum
    }
}

/**
 * Represents a file system event from watching a directory.
 * Used by file-based providers to detect external changes.
 */
sealed class FileEvent {
    abstract val path: String
    abstract val timestamp: Instant

    /**
     * A new file was created.
     */
    data class Created(
        override val path: String,
        override val timestamp: Instant = Instant.now()
    ) : FileEvent()

    /**
     * An existing file was modified.
     */
    data class Modified(
        override val path: String,
        override val timestamp: Instant = Instant.now()
    ) : FileEvent()

    /**
     * A file was deleted.
     */
    data class Deleted(
        override val path: String,
        override val timestamp: Instant = Instant.now()
    ) : FileEvent()

    /**
     * A file was moved or renamed.
     */
    data class Moved(
        val fromPath: String,
        override val path: String,
        override val timestamp: Instant = Instant.now()
    ) : FileEvent()
}

/**
 * Metadata about a note in the provider's storage.
 * Lightweight representation without full content.
 */
data class NoteMetadata(
    val id: String,
    val title: String?,
    val path: String,
    val modifiedAt: Instant,
    val size: Long,
    val tags: List<String> = emptyList()
) {
    companion object {
        /**
         * Generate a note ID from a file path.
         * This creates a stable ID that works across providers.
         */
        fun generateIdFromPath(path: String): String {
            // Remove extension and use path as ID
            return path.removeSuffix(".md")
                .removeSuffix(".txt")
                .replace("/", "_")
                .replace("\\", "_")
        }
    }
}

/**
 * Configuration for file pattern matching.
 * Used to filter files when listing or watching.
 */
data class FilePattern(
    val extension: String = "md",
    val includeSubdirectories: Boolean = true,
    val excludePatterns: List<String> = listOf(
        ".trash",
        ".obsidian",
        ".git"
    ),
    val maxDepth: Int? = null
) {
    /**
     * Check if a path matches this pattern.
     */
    fun matches(path: String): Boolean {
        // Check extension
        if (!path.endsWith(".$extension")) return false

        // Check excluded patterns
        if (excludePatterns.any { pattern -> path.contains(pattern, ignoreCase = true) }) {
            return false
        }

        // Check max depth
        if (maxDepth != null) {
            val depth = path.count { it == '/' || it == '\\' }
            if (depth > maxDepth) return false
        }

        return true
    }
}
