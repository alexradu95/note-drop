package app.notedrop.android.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.notedrop.android.data.voice.RecordingState
import app.notedrop.android.domain.model.Template
import app.notedrop.android.util.rememberAudioPermissionState

/**
 * Quick Capture screen for creating notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureScreen(
    onNavigateBack: () -> Unit,
    onNoteSaved: () -> Unit,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val audioPermissionState = rememberAudioPermissionState()

    // Navigate back when note is saved
    LaunchedEffect(uiState.noteSaved) {
        if (uiState.noteSaved) {
            onNoteSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Capture") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveNote() },
                        enabled = uiState.content.isNotBlank() && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (recordingState) {
                        is RecordingState.Idle -> {
                            if (audioPermissionState.isGranted) {
                                viewModel.startRecording()
                            } else {
                                audioPermissionState.requestPermission()
                            }
                        }
                        is RecordingState.Recording -> viewModel.stopRecording()
                        else -> {}
                    }
                }
            ) {
                Icon(
                    imageVector = when (recordingState) {
                        is RecordingState.Recording -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice recording"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Template selector
            if (templates.isNotEmpty()) {
                Text(
                    text = "Templates",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        TemplateChip(
                            template = template,
                            isSelected = uiState.selectedTemplate?.id == template.id,
                            onClick = { viewModel.onTemplateSelected(template) }
                        )
                    }
                }
            }

            // Content field
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                label = { Text("Note content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                minLines = 8,
                maxLines = 20
            )

            // Recording indicator
            if (recordingState is RecordingState.Recording) {
                RecordingIndicator()
            }

            // Voice recording display
            if (uiState.voiceRecordingPath != null) {
                VoiceRecordingCard(
                    onDelete = { viewModel.cancelRecording() }
                )
            }

            // Tags input
            TagsInput(
                tags = uiState.tags,
                onTagAdded = viewModel::onTagAdded,
                onTagRemoved = viewModel::onTagRemoved
            )

            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TemplateChip(
    template: Template,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(template.name) },
        leadingIcon = if (template.isBuiltIn) {
            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null
    )
}

@Composable
private fun RecordingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Circle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp)
            )
            Text(
                "Recording...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun VoiceRecordingCard(
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Text("Voice recording attached")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete recording")
            }
        }
    }
}

@Composable
private fun TagsInput(
    tags: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.labelLarge
        )

        // Tag chips
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onTagRemoved(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Tag input
        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            label = { Text("Add tag") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            onTagAdded(tagInput)
                            tagInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag")
                }
            }
        )
    }
}
