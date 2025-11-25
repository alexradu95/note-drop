package app.notedrop.android.ui.widget.action

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ProviderFactory
import app.notedrop.android.ui.theme.NoteDropTheme
import app.notedrop.android.ui.widget.InteractiveQuickCaptureWidget
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Dialog-style activity for quick text input from widget
 *
 * Features:
 * - Appears as overlay/dialog
 * - Simple text input
 * - Quick save button
 * - Closes automatically after save
 */
@AndroidEntryPoint
class TextInputActivity : ComponentActivity() {

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var providerFactory: ProviderFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity appear as dialog
        setFinishOnTouchOutside(true)

        setContent {
            NoteDropTheme {
                var text by remember { mutableStateOf("") }
                var isSaving by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Quick Note",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    placeholder = { Text("Type your note here...") },
                                    maxLines = 8,
                                    enabled = !isSaving
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { finish() },
                                        enabled = !isSaving
                                    ) {
                                        Text("Cancel")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            if (text.isNotBlank()) {
                                                isSaving = true
                                                saveNote(text)
                                            }
                                        },
                                        enabled = text.isNotBlank() && !isSaving
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveNote(content: String) {
        lifecycleScope.launch {
            try {
                // Get default vault
                val vault = vaultRepository.getDefaultVault().getOrElse { error ->
                    android.util.Log.e("TextInputActivity", "Failed to get default vault: $error")
                    finish()
                    return@launch
                }

                if (vault == null) {
                    // No default vault configured, still finish activity
                    finish()
                    return@launch
                }

                // Create and save note
                val note = Note(
                    content = content,
                    title = null,
                    vaultId = vault.id,
                    tags = emptyList(),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                val result = noteRepository.createNote(note)

                result.onSuccess { savedNote ->
                    android.util.Log.d("TextInputActivity", "Note saved to database: ${savedNote.id}")

                    // Sync to provider if configured
                    val noteProvider = providerFactory.getProvider(vault.providerType)
                    android.util.Log.d("TextInputActivity", "Got provider: ${noteProvider.javaClass.simpleName}")

                    val isAvailable = noteProvider.isAvailable(vault)
                    android.util.Log.d("TextInputActivity", "Provider available: $isAvailable")

                    if (isAvailable) {
                        android.util.Log.d("TextInputActivity", "Calling provider.saveNote()")
                        val providerResult = noteProvider.saveNote(savedNote, vault)
                        providerResult.onSuccess { filePath ->
                            android.util.Log.d("TextInputActivity", "Provider save success: $filePath")
                            // Update the note with the file path
                            val updatedNote = savedNote.copy(
                                filePath = filePath,
                                isSynced = true
                            )
                            noteRepository.updateNote(updatedNote).onFailure { updateError ->
                                android.util.Log.e("TextInputActivity", "Failed to update note: $updateError")
                            }
                        }.onFailure { providerError ->
                            android.util.Log.e("TextInputActivity", "Failed to save to provider", providerError)
                        }
                    } else {
                        android.util.Log.w("TextInputActivity", "Provider not available, skipping sync")
                    }

                    // Update widget to show saved text (preview)
                    val glanceIdString = intent.getStringExtra("glance_id")
                    if (glanceIdString != null) {
                        val glanceManager = GlanceAppWidgetManager(this@TextInputActivity)
                        val glanceIds = glanceManager.getGlanceIds(InteractiveQuickCaptureWidget::class.java)

                        glanceIds.forEach { glanceId ->
                            updateAppWidgetState(this@TextInputActivity, glanceId) { prefs ->
                                prefs[InteractiveQuickCaptureWidget.WIDGET_TEXT_KEY] =
                                    content.take(50) + if (content.length > 50) "..." else ""
                            }
                            InteractiveQuickCaptureWidget().update(this@TextInputActivity, glanceId)
                        }
                    }
                }

                // Close activity
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                finish()
            }
        }
    }
}
