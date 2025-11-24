package app.notedrop.android.domain.sync

import app.notedrop.android.domain.model.ConflictStrategy
import app.notedrop.android.domain.model.Note

/**
 * Handles resolution of sync conflicts between local and remote notes.
 * Provider-agnostic - works with any note provider.
 */
interface ConflictResolver {

    /**
     * Resolve a conflict between local and remote versions of a note.
     *
     * @param local The local version of the note
     * @param remote The remote version of the note
     * @param strategy The conflict resolution strategy to use
     * @return The resolution result
     */
    fun resolve(
        local: Note,
        remote: Note,
        strategy: ConflictStrategy
    ): ConflictResolution

    /**
     * Attempt to merge changes from both versions.
     * Returns null if automatic merge is not possible.
     *
     * @param local The local version of the note
     * @param remote The remote version of the note
     * @return Merged note or null if merge failed
     */
    fun tryMerge(local: Note, remote: Note): Note?
}

/**
 * Result of conflict resolution.
 */
sealed class ConflictResolution {
    /**
     * Use the local version.
     */
    data class UseLocal(val note: Note) : ConflictResolution()

    /**
     * Use the remote version.
     */
    data class UseRemote(val note: Note) : ConflictResolution()

    /**
     * Keep both versions (remote becomes "Note (conflict).md").
     */
    data class KeepBoth(val local: Note, val remote: Note) : ConflictResolution()

    /**
     * Successfully merged both versions.
     */
    data class Merged(val note: Note) : ConflictResolution()

    /**
     * Conflict requires manual resolution by user.
     */
    data class RequiresManual(val local: Note, val remote: Note, val reason: String) : ConflictResolution()
}
