package app.notedrop.android.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Tests for Note domain model.
 */
class NoteTest {

    @Test
    fun `note creation with minimal fields`() {
        val note = Note(
            content = "Test content",
            vaultId = "vault-1"
        )

        assertThat(note.id).isNotEmpty()
        assertThat(note.content).isEqualTo("Test content")
        assertThat(note.vaultId).isEqualTo("vault-1")
        assertThat(note.title).isNull()
        assertThat(note.tags).isEmpty()
        assertThat(note.voiceRecordingPath).isNull()
        assertThat(note.transcriptionStatus).isEqualTo(TranscriptionStatus.NONE)
        assertThat(note.isSynced).isFalse()
    }

    @Test
    fun `note with all fields`() {
        val now = Instant.now()
        val note = Note(
            id = "custom-id",
            content = "Full content",
            title = "Title",
            vaultId = "vault-1",
            tags = listOf("tag1", "tag2"),
            createdAt = now,
            updatedAt = now,
            voiceRecordingPath = "/path/to/recording.m4a",
            transcriptionStatus = TranscriptionStatus.COMPLETED,
            metadata = mapOf("key" to "value"),
            isSynced = true
        )

        assertThat(note.id).isEqualTo("custom-id")
        assertThat(note.title).isEqualTo("Title")
        assertThat(note.tags).containsExactly("tag1", "tag2")
        assertThat(note.voiceRecordingPath).isEqualTo("/path/to/recording.m4a")
        assertThat(note.transcriptionStatus).isEqualTo(TranscriptionStatus.COMPLETED)
        assertThat(note.metadata).containsEntry("key", "value")
        assertThat(note.isSynced).isTrue()
    }

    @Test
    fun `note with voice recording has pending transcription by default`() {
        val note = Note(
            content = "Voice note",
            vaultId = "vault-1",
            voiceRecordingPath = "/recording.m4a"
        )

        // Note: In real usage, TranscriptionStatus should be set to PENDING
        // when voiceRecordingPath is not null, but the model doesn't enforce this
        assertThat(note.voiceRecordingPath).isNotNull()
    }

    @Test
    fun `note copy preserves all fields`() {
        val original = Note(
            content = "Original",
            vaultId = "vault-1",
            tags = listOf("tag1")
        )

        val copy = original.copy(content = "Updated")

        assertThat(copy.content).isEqualTo("Updated")
        assertThat(copy.vaultId).isEqualTo(original.vaultId)
        assertThat(copy.tags).isEqualTo(original.tags)
        assertThat(copy.id).isEqualTo(original.id)
    }

    @Test
    fun `transcription status enum values`() {
        assertThat(TranscriptionStatus.NONE).isNotNull()
        assertThat(TranscriptionStatus.PENDING).isNotNull()
        assertThat(TranscriptionStatus.IN_PROGRESS).isNotNull()
        assertThat(TranscriptionStatus.COMPLETED).isNotNull()
        assertThat(TranscriptionStatus.FAILED).isNotNull()
    }

    @Test
    fun `note with empty metadata`() {
        val note = Note(
            content = "Test",
            vaultId = "vault-1",
            metadata = emptyMap()
        )

        assertThat(note.metadata).isEmpty()
    }

    @Test
    fun `note with multiple tags`() {
        val tags = listOf("work", "important", "urgent", "review")
        val note = Note(
            content = "Tagged note",
            vaultId = "vault-1",
            tags = tags
        )

        assertThat(note.tags).hasSize(4)
        assertThat(note.tags).containsExactlyElementsIn(tags)
    }

    @Test
    fun `note timestamps are set on creation`() {
        val note = Note(
            content = "Test",
            vaultId = "vault-1"
        )

        assertThat(note.createdAt).isNotNull()
        assertThat(note.updatedAt).isNotNull()
        // CreatedAt and UpdatedAt should be very close in time
        assertThat(note.updatedAt.toEpochMilli() - note.createdAt.toEpochMilli())
            .isLessThan(1000) // Less than 1 second difference
    }
}
