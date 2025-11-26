package app.notedrop.android.domain.usecase

import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.TranscriptionStatus
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import java.time.Instant
import javax.inject.Inject

/**
 * Unified use case for creating notes across the app and widgets.
 *
 * VAULT-ONLY ARCHITECTURE: This use case writes directly to the vault markdown files
 * without any database intermediary. The vault is the single source of truth.
 *
 * This ensures both the app and widgets use the exact same logic for:
 * - Vault retrieval and configuration
 * - Note creation and validation
 * - Direct markdown file writing
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
 * result.onSuccess { filePath ->
 *     // Note created successfully at filePath in vault
 * }.onFailure { error ->
 *     // Handle error
 * }
 * ```
 */
class CreateNoteUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val providerFactory: ProviderFactory
) {
    /**
     * Creates a new note and writes it directly to the vault.
     *
     * VAULT-ONLY: No database intermediary - writes directly to markdown file.
     *
     * @param content The note content (required, must not be blank)
     * @param title Optional note title
     * @param tags List of tags to associate with the note
     * @param voiceRecordingPath Optional path to voice recording in vault
     * @return Result containing the file path where note was created, or an error
     */
    suspend operator fun invoke(
        content: String,
        title: String? = null,
        tags: List<String> = emptyList(),
        voiceRecordingPath: String? = null
    ): Result<String, AppError> {
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

        // Create note object (for structure, not persisted to DB)
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

        // Write directly to vault using provider
        val noteProvider = providerFactory.getProvider(vault.providerType)
        android.util.Log.d(TAG, "Got provider: ${noteProvider.javaClass.simpleName}")

        val isAvailable = noteProvider.isAvailable(vault)
        android.util.Log.d(TAG, "Provider available: $isAvailable")

        if (!isAvailable) {
            android.util.Log.e(TAG, "Provider not available")
            return Err(AppError.Sync.PushFailed("Vault provider is not available"))
        }

        // Save note directly to vault (no DB)
        android.util.Log.d(TAG, "Writing note to vault...")
        val providerResult = noteProvider.saveNote(note, vault)

        // Convert kotlin.Result to com.github.michaelbull.result.Result
        return providerResult.fold(
            onSuccess = { filePath ->
                android.util.Log.d(TAG, "Note saved to vault successfully: $filePath")
                Ok(filePath)
            },
            onFailure = { error ->
                android.util.Log.e(TAG, "Failed to save note to vault", error)
                Err(AppError.FileSystem.WriteError(vault.name, error))
            }
        )
    }

    companion object {
        private const val TAG = "CreateNoteUseCase"
    }
}
