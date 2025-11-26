package app.notedrop.android.domain.usecase

import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.TranscriptionStatus
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import java.time.Instant
import javax.inject.Inject

/**
 * Unified use case for creating notes across the app and widgets.
 *
 * This ensures both the app and widgets use the exact same logic for:
 * - Vault retrieval
 * - Note creation
 * - Database persistence
 * - Provider synchronization
 * - Error handling
 *
 * Usage:
 * ```
 * val result = createNoteUseCase(
 *     content = "My note",
 *     title = "Optional title",
 *     tags = listOf("tag1", "tag2"),
 *     voiceRecordingPath = "attachments/recording.m4a"
 * )
 *
 * result.onSuccess { note ->
 *     // Note created successfully
 * }.onFailure { error ->
 *     // Handle error
 * }
 * ```
 */
class CreateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
    private val vaultRepository: VaultRepository,
    private val providerFactory: ProviderFactory
) {
    /**
     * Creates a new note and syncs it to the configured provider.
     *
     * @param content The note content (required, must not be blank)
     * @param title Optional note title
     * @param tags List of tags to associate with the note
     * @param voiceRecordingPath Optional path to voice recording in vault
     * @return Result containing the created Note or an error
     */
    suspend operator fun invoke(
        content: String,
        title: String? = null,
        tags: List<String> = emptyList(),
        voiceRecordingPath: String? = null
    ): Result<Note, AppError> {
        // Validate content
        if (content.isBlank()) {
            return Err(AppError.Validation.FieldError("content", "Note content cannot be empty"))
        }

        // Get default vault
        val vault = vaultRepository.getDefaultVault().getOrElse { error ->
            android.util.Log.e(TAG, "Failed to get default vault: $error")
            return Err(error)
        }

        if (vault == null) {
            android.util.Log.e(TAG, "No default vault configured")
            return Err(AppError.Sync.NoVaultConfigured)
        }

        android.util.Log.d(TAG, "Creating note in vault: ${vault.name}")

        // Create note
        val note = Note(
            content = content,
            title = title?.takeIf { it.isNotBlank() },
            vaultId = vault.id,
            tags = tags,
            voiceRecordingPath = voiceRecordingPath,
            transcriptionStatus = if (voiceRecordingPath != null) {
                TranscriptionStatus.PENDING
            } else {
                TranscriptionStatus.NONE
            },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Save to local database
        val createResult = noteRepository.createNote(note)

        return createResult.onSuccess { savedNote ->
            android.util.Log.d(TAG, "Note saved to database: ${savedNote.id}")

            // Sync to provider if configured
            val noteProvider = providerFactory.getProvider(vault.providerType)
            android.util.Log.d(TAG, "Got provider: ${noteProvider.javaClass.simpleName}")

            val isAvailable = noteProvider.isAvailable(vault)
            android.util.Log.d(TAG, "Provider available: $isAvailable")

            if (isAvailable) {
                android.util.Log.d(TAG, "Syncing note to provider...")
                val providerResult = noteProvider.saveNote(savedNote, vault)

                providerResult.onSuccess { filePath ->
                    android.util.Log.d(TAG, "Provider save success: $filePath")

                    // Update the note with the file path
                    val updatedNote = savedNote.copy(
                        filePath = filePath,
                        isSynced = true
                    )

                    noteRepository.updateNote(updatedNote).onFailure { updateError ->
                        android.util.Log.e(TAG, "Failed to update note with file path: $updateError")
                    }
                }.onFailure { providerError ->
                    android.util.Log.e(TAG, "Failed to sync to provider: $providerError")
                    // Note: We don't return error here because the note was saved to DB
                }
            } else {
                android.util.Log.w(TAG, "Provider not available, note saved to database only")
            }
        }
    }

    companion object {
        private const val TAG = "CreateNoteUseCase"
    }
}
