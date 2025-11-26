package app.notedrop.android.ui.home

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import app.notedrop.android.domain.model.Note
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.SyncStatus
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Home screen showing notes and daily notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onQuickCaptureClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todaysNotes by viewModel.todaysNotes.collectAsState()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val defaultVault by viewModel.defaultVault.collectAsState()
    val allVaults by viewModel.allVaults.collectAsState()
    val selectedVault by viewModel.selectedVault.collectAsState()
    val syncStatesMap by viewModel.syncStatesMap.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NoteDrop") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQuickCaptureClick
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Capture")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vault selector
            if (allVaults.isNotEmpty()) {
                VaultSelector(
                    vaults = allVaults,
                    selectedVault = selectedVault ?: defaultVault,
                    onVaultSelected = { viewModel.selectVault(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::clearSearch,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterChange = viewModel::onFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Today's notes section (only shown when filter is ALL or TODAY)
            if (todaysNotes.isNotEmpty() && (selectedFilter == NoteFilter.ALL || selectedFilter == NoteFilter.TODAY)) {
                Text(
                    text = "Today's Notes (${todaysNotes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Notes list
            if (filteredNotes.isEmpty()) {
                EmptyState(
                    filter = selectedFilter,
                    hasSearchQuery = searchQuery.isNotBlank()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredNotes,
                        key = { it.id }
                    ) { note ->
                        val syncState = syncStatesMap[note.id]
                        // Get the vault for this note
                        val noteVault = allVaults.find { it.id == note.vaultId }
                        NoteCard(
                            note = note,
                            vault = noteVault,
                            syncState = syncState,
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search notes...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun FilterChips(
    selectedFilter: NoteFilter,
    onFilterChange: (NoteFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoteFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        when (filter) {
                            NoteFilter.ALL -> "All"
                            NoteFilter.TODAY -> "Today"
                            NoteFilter.WITH_VOICE -> "Voice"
                            NoteFilter.TAGGED -> "Tagged"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    vault: app.notedrop.android.domain.model.Vault?,
    syncState: app.notedrop.android.domain.model.SyncState?,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with timestamp, sync status, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(note.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Sync status indicator
                    SyncStatusIcon(syncState)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title
            note.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Content
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Voice recording player
            note.voiceRecordingPath?.let { audioPath ->
                vault?.let { v ->
                    VoiceRecordingPlayer(audioPath = audioPath, vault = v)
                }
            }

            // Display images from markdown content
            val imageLinks = extractImageLinks(note.content)
            if (imageLinks.isNotEmpty() && vault != null) {
                NoteImagesDisplay(imageLinks = imageLinks.take(2), vault = vault) // Show max 2 images
            }

            // File path
            note.filePath?.let { filePath ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = filePath,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer with tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tags
                note.tags.take(3).forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }

                if (note.tags.size > 3) {
                    Text(
                        text = "+${note.tags.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Sync status icon indicator.
 */
@Composable
private fun SyncStatusIcon(syncState: app.notedrop.android.domain.model.SyncState?) {
    val (icon, tint, description) = when (syncState?.status) {
        SyncStatus.SYNCED -> Triple(
            Icons.Default.CloudDone,
            MaterialTheme.colorScheme.primary,
            "Synced"
        )
        SyncStatus.PENDING_UPLOAD -> Triple(
            Icons.Default.CloudUpload,
            MaterialTheme.colorScheme.tertiary,
            "Syncing..."
        )
        SyncStatus.PENDING_DOWNLOAD -> Triple(
            Icons.Default.CloudDownload,
            MaterialTheme.colorScheme.tertiary,
            "Downloading..."
        )
        SyncStatus.ERROR -> Triple(
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.error,
            "Sync failed: ${syncState.errorMessage ?: "Unknown error"}"
        )
        SyncStatus.CONFLICT -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error,
            "Sync conflict"
        )
        SyncStatus.NEVER_SYNCED, null -> Triple(
            Icons.Default.CloudQueue,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            "Not synced"
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = Modifier.size(14.dp),
        tint = tint
    )
}

@Composable
private fun EmptyState(
    filter: NoteFilter,
    hasSearchQuery: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when {
                    hasSearchQuery -> "No notes found"
                    filter == NoteFilter.TODAY -> "No notes today"
                    filter == NoteFilter.WITH_VOICE -> "No voice notes"
                    filter == NoteFilter.TAGGED -> "No tagged notes"
                    else -> "No notes yet"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasSearchQuery && filter == NoteFilter.ALL) {
                Text(
                    text = "Tap + to create your first note",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSelector(
    vaults: List<app.notedrop.android.domain.model.Vault>,
    selectedVault: app.notedrop.android.domain.model.Vault?,
    onVaultSelected: (app.notedrop.android.domain.model.Vault) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        onClick = { expanded = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Column {
                    Text(
                        text = selectedVault?.name ?: "Select Vault",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (vaults.size > 1) {
                        Text(
                            text = "${vaults.size} vaults available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            vaults.forEach { vault ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vault.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                vault.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (vault.id == selectedVault?.id) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    onClick = {
                        onVaultSelected(vault)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Voice recording player with waveform visualization
 */
@Composable
private fun VoiceRecordingPlayer(audioPath: String, vault: app.notedrop.android.domain.model.Vault) {
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }
    val context = LocalContext.current

    // Get the audio file URI from vault
    val audioUri = remember(audioPath, vault) {
        val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
        if (config != null) {
            try {
                val vaultUri = Uri.parse(config.vaultPath)
                val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                if (vaultRoot != null) {
                    // Navigate to the audio file
                    val pathParts = audioPath.split("/")
                    var currentDir: DocumentFile? = vaultRoot

                    // Navigate through directories
                    for (i in 0 until pathParts.size - 1) {
                        currentDir = currentDir?.findFile(pathParts[i])
                        if (currentDir == null) break
                    }

                    // Find the audio file
                    val audioFile = currentDir?.findFile(pathParts.last())
                    audioFile?.uri
                } else null
            } catch (e: Exception) {
                android.util.Log.e("VoicePlayer", "Error finding audio file", e)
                null
            }
        } else null
    }

    DisposableEffect(audioPath) {
        onDispose {
            if (isPlaying) {
                try {
                    mediaPlayer.stop()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            mediaPlayer.release()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            IconButton(
                onClick = {
                    if (isPlaying) {
                        try {
                            mediaPlayer.pause()
                            isPlaying = false
                        } catch (e: Exception) {
                            android.util.Log.e("VoicePlayer", "Error pausing audio", e)
                            isPlaying = false
                        }
                    } else {
                        if (audioUri != null) {
                            try {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(context, audioUri)
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                                isPlaying = true
                                mediaPlayer.setOnCompletionListener {
                                    isPlaying = false
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("VoicePlayer", "Error playing audio: ${e.message}", e)
                            }
                        } else {
                            android.util.Log.e("VoicePlayer", "Audio URI is null, cannot play")
                        }
                    }
                },
                enabled = audioUri != null
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (audioUri != null)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
            }

            // Waveform visualization (simplified)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Simple waveform bars
                repeat(20) { index ->
                    val height = remember { (10..30).random() }
                    Surface(
                        modifier = Modifier
                            .width(3.dp)
                            .height(height.dp),
                        color = if (isPlaying && index % 2 == 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }

            // Audio indicator icon
            Icon(
                Icons.Default.Mic,
                contentDescription = "Voice recording",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Display images from markdown content
 */
@Composable
private fun NoteImagesDisplay(imageLinks: List<String>, vault: app.notedrop.android.domain.model.Vault) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imageLinks.forEach { imagePath ->
            // Resolve image URI from vault
            val imageUri = remember(imagePath, vault) {
                val config = vault.providerConfig as? ProviderConfig.ObsidianConfig
                if (config != null) {
                    try {
                        val vaultUri = Uri.parse(config.vaultPath)
                        val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
                        if (vaultRoot != null) {
                            // Navigate to the image file
                            val pathParts = imagePath.split("/")
                            var currentDir: DocumentFile? = vaultRoot

                            // Navigate through directories
                            for (i in 0 until pathParts.size - 1) {
                                currentDir = currentDir?.findFile(pathParts[i])
                                if (currentDir == null) break
                            }

                            // Find the image file
                            val imageFile = currentDir?.findFile(pathParts.last())
                            imageFile?.uri
                        } else null
                    } catch (e: Exception) {
                        android.util.Log.e("ImageDisplay", "Error finding image file", e)
                        null
                    }
                } else null
            }

            if (imageUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Note image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * Extract image links from markdown content
 * Supports both ![alt](path) and ![[path]] syntax
 */
private fun extractImageLinks(content: String): List<String> {
    val imageLinks = mutableListOf<String>()

    // Extract ![alt](path) syntax
    val markdownRegex = """!\[.*?]\((.*?)\)""".toRegex()
    markdownRegex.findAll(content).forEach { match ->
        match.groupValues.getOrNull(1)?.let { imageLinks.add(it) }
    }

    // Extract ![[path]] syntax (Obsidian wikilinks)
    val wikilinksRegex = """!\[\[(.*?)\]\]""".toRegex()
    wikilinksRegex.findAll(content).forEach { match ->
        match.groupValues.getOrNull(1)?.let { imageLinks.add(it) }
    }

    return imageLinks
}

/**
 * Format timestamp for display.
 */
private fun formatTimestamp(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
