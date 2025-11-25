package app.notedrop.android.domain.model

/**
 * Sealed class hierarchy representing all possible errors in the application.
 *
 * This provides type-safe error handling throughout the application layers.
 * Used with kotlin-result's Result<T, E> type for functional error handling.
 */
sealed class AppError {

    /**
     * Database-related errors.
     */
    sealed class Database : AppError() {
        /**
         * Error occurred during insert operation.
         *
         * @property message Error description
         * @property cause Original exception if available
         */
        data class InsertError(
            val message: String,
            val cause: Throwable? = null
        ) : Database()

        /**
         * Error occurred during update operation.
         */
        data class UpdateError(
            val message: String,
            val cause: Throwable? = null
        ) : Database()

        /**
         * Error occurred during delete operation.
         */
        data class DeleteError(
            val message: String,
            val cause: Throwable? = null
        ) : Database()

        /**
         * Requested entity not found in database.
         *
         * @property entityType Type of entity (e.g., "Note", "Vault")
         * @property id Entity identifier
         */
        data class NotFound(
            val entityType: String,
            val id: String
        ) : Database()

        /**
         * Database constraint violation (e.g., unique constraint, foreign key).
         */
        data class ConstraintViolation(
            val message: String,
            val cause: Throwable? = null
        ) : Database()
    }

    /**
     * Synchronization-related errors.
     */
    sealed class Sync : AppError() {
        /**
         * Sync conflict detected between local and remote.
         *
         * @property noteId ID of the conflicting note
         * @property localContent Local version content
         * @property remoteContent Remote version content
         */
        data class ConflictDetected(
            val noteId: String,
            val localContent: String,
            val remoteContent: String
        ) : Sync()

        /**
         * Failed to push changes to remote.
         */
        data class PushFailed(
            val message: String,
            val cause: Throwable? = null
        ) : Sync()

        /**
         * Failed to pull changes from remote.
         */
        data class PullFailed(
            val message: String,
            val cause: Throwable? = null
        ) : Sync()

        /**
         * Sync operation timed out.
         */
        data class Timeout(
            val timeoutMs: Long
        ) : Sync()

        /**
         * No vault configured for sync.
         */
        data object NoVaultConfigured : Sync()
    }

    /**
     * File system and storage errors.
     */
    sealed class FileSystem : AppError() {
        /**
         * File or directory not found.
         *
         * @property path Path that was not found
         */
        data class NotFound(val path: String) : FileSystem()

        /**
         * Permission denied for file operation.
         */
        data class PermissionDenied(
            val path: String,
            val operation: String
        ) : FileSystem()

        /**
         * Error reading file.
         */
        data class ReadError(
            val path: String,
            val cause: Throwable? = null
        ) : FileSystem()

        /**
         * Error writing file.
         */
        data class WriteError(
            val path: String,
            val cause: Throwable? = null
        ) : FileSystem()

        /**
         * Insufficient storage space.
         */
        data class InsufficientSpace(
            val requiredBytes: Long,
            val availableBytes: Long
        ) : FileSystem()

        /**
         * Invalid file path or name.
         */
        data class InvalidPath(
            val path: String,
            val reason: String
        ) : FileSystem()
    }

    /**
     * Input validation errors.
     */
    sealed class Validation : AppError() {
        /**
         * Field validation failed.
         *
         * @property field Field name that failed validation
         * @property message Validation error message
         */
        data class FieldError(
            val field: String,
            val message: String
        ) : Validation()

        /**
         * Required field is missing.
         */
        data class MissingField(
            val field: String
        ) : Validation()

        /**
         * Field value is invalid.
         */
        data class InvalidValue(
            val field: String,
            val value: String,
            val reason: String
        ) : Validation()

        /**
         * Multiple validation errors.
         */
        data class MultipleErrors(
            val errors: List<Validation>
        ) : Validation()
    }

    /**
     * Voice recording and transcription errors.
     */
    sealed class Voice : AppError() {
        /**
         * Microphone permission not granted.
         */
        data object PermissionDenied : Voice()

        /**
         * Error starting recording.
         */
        data class RecordingFailed(
            val message: String,
            val cause: Throwable? = null
        ) : Voice()

        /**
         * Error during transcription.
         */
        data class TranscriptionFailed(
            val message: String,
            val cause: Throwable? = null
        ) : Voice()

        /**
         * Audio file corrupted or invalid.
         */
        data class InvalidAudioFile(
            val path: String
        ) : Voice()
    }

    /**
     * Network-related errors.
     */
    sealed class Network : AppError() {
        /**
         * No internet connection available.
         */
        data object NoConnection : Network()

        /**
         * Request timed out.
         */
        data class Timeout(val timeoutMs: Long) : Network()

        /**
         * Server returned an error.
         */
        data class ServerError(
            val code: Int,
            val message: String
        ) : Network()
    }

    /**
     * Unknown or unexpected errors.
     *
     * Use this as a fallback when the error doesn't fit other categories.
     */
    data class Unknown(
        val message: String,
        val cause: Throwable? = null
    ) : AppError()
}

/**
 * Extension function to convert AppError to user-friendly message.
 */
fun AppError.toUserMessage(): String = when (this) {
    is AppError.Database.NotFound -> "Could not find $entityType with ID: $id"
    is AppError.Database.InsertError -> "Failed to save: $message"
    is AppError.Database.UpdateError -> "Failed to update: $message"
    is AppError.Database.DeleteError -> "Failed to delete: $message"
    is AppError.Database.ConstraintViolation -> "Database constraint violated: $message"

    is AppError.Sync.ConflictDetected -> "Sync conflict detected for note $noteId"
    is AppError.Sync.PushFailed -> "Failed to upload changes: $message"
    is AppError.Sync.PullFailed -> "Failed to download changes: $message"
    is AppError.Sync.Timeout -> "Sync timed out after ${timeoutMs}ms"
    is AppError.Sync.NoVaultConfigured -> "No vault configured. Please create a vault first."

    is AppError.FileSystem.NotFound -> "File not found: $path"
    is AppError.FileSystem.PermissionDenied -> "Permission denied for $operation on $path"
    is AppError.FileSystem.ReadError -> "Failed to read file: $path"
    is AppError.FileSystem.WriteError -> "Failed to write file: $path"
    is AppError.FileSystem.InsufficientSpace -> "Not enough storage space"
    is AppError.FileSystem.InvalidPath -> "Invalid path: $reason"

    is AppError.Validation.FieldError -> "$field: $message"
    is AppError.Validation.MissingField -> "$field is required"
    is AppError.Validation.InvalidValue -> "$field is invalid: $reason"
    is AppError.Validation.MultipleErrors -> errors.joinToString("\n") { it.toUserMessage() }

    is AppError.Voice.PermissionDenied -> "Microphone permission required"
    is AppError.Voice.RecordingFailed -> "Recording failed: $message"
    is AppError.Voice.TranscriptionFailed -> "Transcription failed: $message"
    is AppError.Voice.InvalidAudioFile -> "Invalid audio file: $path"

    is AppError.Network.NoConnection -> "No internet connection"
    is AppError.Network.Timeout -> "Request timed out"
    is AppError.Network.ServerError -> "Server error ($code): $message"

    is AppError.Unknown -> "An unexpected error occurred: $message"
}
