package app.notedrop.android.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.domain.model.Vault

/**
 * Settings screen for managing vaults and app settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val vaults by viewModel.vaults.collectAsState()
    val defaultVault by viewModel.defaultVault.collectAsState()

    var showCreateVaultDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateVaultDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Connect Vault") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Vaults",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Vaults list
            if (vaults.isEmpty()) {
                item {
                    EmptyVaultState()
                }
            } else {
                items(vaults) { vault ->
                    val isDefault = vault.id == defaultVault?.id
                    android.util.Log.d("SettingsScreen", "Rendering vault: id=${vault.id}, name=${vault.name}, isDefault=$isDefault, defaultVaultId=${defaultVault?.id}")
                    VaultCard(
                        vault = vault,
                        isDefault = isDefault,
                        onSetDefault = {
                            android.util.Log.d("SettingsScreen", "VaultCard - Set as Default clicked for vault: ${vault.id}")
                            viewModel.setDefaultVault(vault.id)
                        },
                        onDelete = { viewModel.deleteVault(vault.id) },
                        onViewConfig = {
                            android.util.Log.d("SettingsScreen", "VaultCard - Configure clicked for vault: ${vault.id}")
                            viewModel.loadVaultConfig(vault)
                        },
                        hasValidConfig = when (val config = vault.providerConfig) {
                            is ProviderConfig.ObsidianConfig ->
                                config.vaultPath.isNotBlank() && config.vaultPath.startsWith("content://")
                            else -> false
                        }
                    )
                }
            }

            // About section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                AboutSection()
            }
        }
    }

    // Create vault dialog
    if (showCreateVaultDialog) {
        CreateVaultDialog(
            onDismiss = {
                showCreateVaultDialog = false
                viewModel.resetState()
            },
            onCreate = { name, description, providerType, vaultPath, setAsDefault ->
                viewModel.createVault(name, description, providerType, vaultPath, setAsDefault)
            }
        )
    }

    // Close dialog when vault is created
    LaunchedEffect(uiState.vaultCreated) {
        if (uiState.vaultCreated) {
            showCreateVaultDialog = false
        }
    }

    // Vault configuration dialog
    val vaultConfig = uiState.vaultConfig
    val currentVault = uiState.currentVault
    if (uiState.showConfigScreen && vaultConfig != null && currentVault != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewModel.dismissConfigScreen() },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            VaultConfigurationScreen(
                vaultConfig = vaultConfig,
                vault = currentVault,
                isDefault = currentVault.id == defaultVault?.id,
                onNavigateBack = {
                    viewModel.dismissConfigScreen()
                },
                onSaveConfig = { config ->
                    // TODO: Save updated config
                    viewModel.dismissConfigScreen()
                },
                onSetDefault = {
                    android.util.Log.d("SettingsScreen", "VaultConfigurationScreen - Set as Default clicked for vault: ${currentVault.id}")
                    viewModel.setDefaultVault(currentVault.id)
                }
            )
        }
    }

    // Error snackbar
    uiState.error?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(error)
        }
    }
}

@Composable
private fun VaultCard(
    vault: Vault,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
    onViewConfig: () -> Unit,
    hasValidConfig: Boolean = true
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (hasValidConfig) {
                    Modifier.clickable(onClick = onViewConfig)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vault.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (isDefault) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Default", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    vault.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Divider()

            // Vault info
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = vault.providerType.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isDefault) {
                    Button(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set as Default")
                    }
                }
                if (hasValidConfig) {
                    OutlinedButton(
                        onClick = onViewConfig,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure")
                    }
                } else {
                    // Show info that vault needs to be reconnected
                    OutlinedButton(
                        onClick = { /* TODO: Show reconnect dialog */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reconnect")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vault") },
            text = { Text("Are you sure you want to delete this vault? All notes in this vault will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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

@Composable
private fun EmptyVaultState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Obsidian vault connected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Connect your Obsidian vault to start capturing notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateVaultDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?, ProviderType, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var vaultPath by remember { mutableStateOf("") }
    var vaultUri by remember { mutableStateOf<Uri?>(null) }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            vaultUri = it

            // Get the folder name from DocumentFile
            val documentFile = DocumentFile.fromTreeUri(context, it)
            val folderName = documentFile?.name ?: "Obsidian Vault"

            // Store the URI as the path (we'll use URI for file operations)
            vaultPath = it.toString()
        }
    }

    // Auto-generate vault name from URI
    val vaultName = remember(vaultUri) {
        vaultUri?.let { uri ->
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.name ?: "Obsidian Vault"
        } ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect Obsidian Vault") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info text
                Text(
                    text = "Select your Obsidian vault folder to start capturing notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Browse button
                OutlinedButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Browse for Vault Folder")
                }

                // Show selected folder
                if (vaultName.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected Vault",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = vaultName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Helper text
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Select the root folder of your Obsidian vault. Notes will be saved as markdown files in this location.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        vaultName.ifBlank { "Obsidian Vault" },
                        null, // No description
                        ProviderType.OBSIDIAN,
                        vaultPath,
                        true // Always set as default
                    )
                },
                enabled = vaultPath.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect Vault")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "NoteDrop",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Privacy-first note capture for Android 12+",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Built with Material You and local-first storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
