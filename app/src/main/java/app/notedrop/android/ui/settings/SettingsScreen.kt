package app.notedrop.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                text = { Text("New Vault") }
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
                    VaultCard(
                        vault = vault,
                        isDefault = vault.id == defaultVault?.id,
                        onSetDefault = { viewModel.setDefaultVault(vault.id) },
                        onDelete = { viewModel.deleteVault(vault.id) }
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
                if (uiState.vaultCreated) {
                    showCreateVaultDialog = false
                }
            }
        )
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

            // Action button
            if (!isDefault) {
                Button(
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Default")
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
                text = "No vaults configured",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create a vault to start capturing notes",
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
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var vaultPath by remember { mutableStateOf("") }
    var setAsDefault by remember { mutableStateOf(true) }
    var selectedProvider by remember { mutableStateOf(ProviderType.OBSIDIAN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Vault") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vault Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true
                )

                Text(
                    text = "Provider",
                    style = MaterialTheme.typography.labelMedium
                )

                ProviderType.values().forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider }
                        )
                        Text(
                            text = provider.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = vaultPath,
                    onValueChange = { vaultPath = it },
                    label = { Text("Vault Path") },
                    singleLine = true,
                    placeholder = { Text("/storage/emulated/0/Documents/MyVault") }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = setAsDefault,
                        onCheckedChange = { setAsDefault = it }
                    )
                    Text(
                        text = "Set as default vault",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        name,
                        description.takeIf { it.isNotBlank() },
                        selectedProvider,
                        vaultPath,
                        setAsDefault
                    )
                },
                enabled = name.isNotBlank() && vaultPath.isNotBlank()
            ) {
                Text("Create")
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
