package app.notedrop.android.domain.sync

import app.notedrop.android.domain.model.ConflictStrategy
import app.notedrop.android.domain.model.Note
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ConflictResolver.
 * Provides various strategies for resolving sync conflicts.
 */
@Singleton
class ConflictResolverImpl @Inject constructor() : ConflictResolver {

    override fun resolve(
        local: Note,
        remote: Note,
        strategy: ConflictStrategy
    ): ConflictResolution {
        return when (strategy) {
            ConflictStrategy.LAST_WRITE_WINS -> resolveLastWriteWins(local, remote)
            ConflictStrategy.KEEP_BOTH -> resolveKeepBoth(local, remote)
            ConflictStrategy.LOCAL_WINS -> ConflictResolution.UseLocal(local)
            ConflictStrategy.REMOTE_WINS -> ConflictResolution.UseRemote(remote)
            ConflictStrategy.MANUAL -> ConflictResolution.RequiresManual(
                local = local,
                remote = remote,
                reason = "Manual resolution required by user"
            )
        }
    }

    override fun tryMerge(local: Note, remote: Note): Note? {
        // Check if notes are actually different
        if (local.content == remote.content &&
            local.title == remote.title &&
            local.tags.toSet() == remote.tags.toSet()
        ) {
            // Notes are identical, no merge needed
            return local.copy(updatedAt = maxOf(local.updatedAt, remote.updatedAt))
        }

        // Check if only metadata changed (easy merge)
        if (local.content == remote.content) {
            return mergeMetadataOnly(local, remote)
        }

        // Check if content changes don't overlap (line-based merge)
        return tryLineMerge(local, remote)
    }

    /**
     * Resolve conflict using last-write-wins strategy.
     * The version with the newer timestamp wins.
     */
    private fun resolveLastWriteWins(local: Note, remote: Note): ConflictResolution {
        return if (local.updatedAt.isAfter(remote.updatedAt)) {
            ConflictResolution.UseLocal(local)
        } else if (remote.updatedAt.isAfter(local.updatedAt)) {
            ConflictResolution.UseRemote(remote)
        } else {
            // Same timestamp - try to merge
            val merged = tryMerge(local, remote)
            if (merged != null) {
                ConflictResolution.Merged(merged)
            } else {
                // Default to local if merge fails
                ConflictResolution.UseLocal(local)
            }
        }
    }

    /**
     * Resolve conflict by keeping both versions.
     * Remote note gets marked as conflict copy.
     */
    private fun resolveKeepBoth(local: Note, remote: Note): ConflictResolution {
        // Modify remote note title to indicate conflict
        val conflictTitle = if (remote.title != null) {
            "${remote.title} (conflict)"
        } else {
            "Conflict copy"
        }

        val conflictRemote = remote.copy(
            title = conflictTitle,
            updatedAt = Instant.now()
        )

        return ConflictResolution.KeepBoth(
            local = local,
            remote = conflictRemote
        )
    }

    /**
     * Merge notes when only metadata changed (content is identical).
     */
    private fun mergeMetadataOnly(local: Note, remote: Note): Note {
        // Merge tags
        val mergedTags = (local.tags + remote.tags).distinct()

        // Merge metadata
        val mergedMetadata = local.metadata + remote.metadata

        // Use most recent title
        val title = if (local.updatedAt.isAfter(remote.updatedAt)) {
            local.title
        } else {
            remote.title
        }

        // Use most recent timestamps
        val createdAt = minOf(local.createdAt, remote.createdAt)
        val updatedAt = maxOf(local.updatedAt, remote.updatedAt)

        return local.copy(
            title = title,
            tags = mergedTags,
            metadata = mergedMetadata,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Attempt a simple line-based merge.
     * Only works if changes are in different parts of the content.
     */
    private fun tryLineMerge(local: Note, remote: Note): Note? {
        // Find a common ancestor (we don't have version history, so use simpler heuristic)
        // If one version is a superset of the other, we can merge

        val localLines = local.content.lines()
        val remoteLines = remote.content.lines()

        // Check if one is a superset (appending case)
        if (localLines.size > remoteLines.size &&
            localLines.take(remoteLines.size) == remoteLines
        ) {
            // Local has appended content
            return local.copy(
                tags = (local.tags + remote.tags).distinct(),
                metadata = local.metadata + remote.metadata,
                updatedAt = maxOf(local.updatedAt, remote.updatedAt)
            )
        }

        if (remoteLines.size > localLines.size &&
            remoteLines.take(localLines.size) == localLines
        ) {
            // Remote has appended content
            return remote.copy(
                id = local.id,
                vaultId = local.vaultId,
                tags = (local.tags + remote.tags).distinct(),
                metadata = local.metadata + remote.metadata,
                updatedAt = maxOf(local.updatedAt, remote.updatedAt)
            )
        }

        // Check if changes are only at the beginning or end
        val commonPrefix = localLines.zip(remoteLines)
            .takeWhile { (a, b) -> a == b }
            .count()

        val localReversed = localLines.reversed()
        val remoteReversed = remoteLines.reversed()
        val commonSuffix = localReversed.zip(remoteReversed)
            .takeWhile { (a, b) -> a == b }
            .count()

        // If we have a significant common section and changes don't overlap
        if (commonPrefix + commonSuffix >= minOf(localLines.size, remoteLines.size) * 0.5) {
            // Attempt merge: common prefix + unique middle sections + common suffix
            val mergedLines = mutableListOf<String>()

            // Add common prefix
            mergedLines.addAll(localLines.take(commonPrefix))

            // Add unique local section
            val localMiddle = localLines.drop(commonPrefix).dropLast(commonSuffix)
            mergedLines.addAll(localMiddle)

            // Add unique remote section
            val remoteMiddle = remoteLines.drop(commonPrefix).dropLast(commonSuffix)
            if (remoteMiddle.isNotEmpty()) {
                mergedLines.add("") // Blank line separator
                mergedLines.addAll(remoteMiddle)
            }

            // Add common suffix
            if (commonSuffix > 0) {
                mergedLines.add("") // Blank line separator
                mergedLines.addAll(localLines.takeLast(commonSuffix))
            }

            return local.copy(
                content = mergedLines.joinToString("\n"),
                tags = (local.tags + remote.tags).distinct(),
                metadata = local.metadata + remote.metadata,
                updatedAt = Instant.now()
            )
        }

        // Cannot safely merge
        return null
    }
}
