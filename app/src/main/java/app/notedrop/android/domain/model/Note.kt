package app.notedrop.android.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model representing a captured note.
 *
 * @property id Unique identifier for the note
 * @property content The text content of the note
 * @property title Optional title (extracted from first line or user-provided)
 * @property vaultId ID of the vault this note belongs to
 * @property tags List of tags associated with the note
 * @property createdAt Timestamp when the note was created
 * @property updatedAt Timestamp when the note was last updated
 * @property voiceRecordingPath Optional path to associated voice recording
 * @property transcriptionStatus Status of voice transcription
 * @property metadata Additional metadata as key-value pairs
 * @property isSynced Whether the note has been synced to external provider
 */
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val title: String? = null,
    val vaultId: String,
    val tags: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val voiceRecordingPath: String? = null,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NONE,
    val metadata: Map<String, String> = emptyMap(),
    val isSynced: Boolean = false
)

/**
 * Status of voice transcription for a note.
 */
enum class TranscriptionStatus {
    NONE,           // No voice recording
    PENDING,        // Voice recording exists, transcription queued
    IN_PROGRESS,    // Transcription in progress
    COMPLETED,      // Transcription completed successfully
    FAILED          // Transcription failed
}
