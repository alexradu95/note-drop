package app.notedrop.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.TranscriptionStatus
import java.time.Instant

/**
 * Room entity for storing notes in the local database.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val title: String?,
    val vaultId: String,
    val tags: String, // Stored as comma-separated values
    val createdAt: Long, // Stored as epoch milliseconds
    val updatedAt: Long,
    val voiceRecordingPath: String?,
    val transcriptionStatus: String,
    val metadata: String, // Stored as JSON string
    val isSynced: Boolean,
    val filePath: String? // Path to the markdown file in the vault
)

/**
 * Convert domain Note to database NoteEntity.
 */
fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        content = content,
        title = title,
        vaultId = vaultId,
        tags = tags.joinToString(","),
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        voiceRecordingPath = voiceRecordingPath,
        transcriptionStatus = transcriptionStatus.name,
        metadata = metadataToJson(metadata),
        isSynced = isSynced,
        filePath = filePath
    )
}

/**
 * Convert database NoteEntity to domain Note.
 */
fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        content = content,
        title = title,
        vaultId = vaultId,
        tags = if (tags.isEmpty()) emptyList() else tags.split(","),
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        voiceRecordingPath = voiceRecordingPath,
        transcriptionStatus = TranscriptionStatus.valueOf(transcriptionStatus),
        metadata = jsonToMetadata(metadata),
        isSynced = isSynced,
        filePath = filePath
    )
}

/**
 * Convert metadata map to JSON string.
 * Simple implementation - can be enhanced with proper JSON library later.
 */
private fun metadataToJson(metadata: Map<String, String>): String {
    if (metadata.isEmpty()) return "{}"
    return metadata.entries.joinToString(",", "{", "}") { (key, value) ->
        "\"$key\":\"$value\""
    }
}

/**
 * Convert JSON string to metadata map.
 * Simple implementation - can be enhanced with proper JSON library later.
 */
private fun jsonToMetadata(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    return emptyMap() // TODO: Implement proper JSON parsing or use library
}
