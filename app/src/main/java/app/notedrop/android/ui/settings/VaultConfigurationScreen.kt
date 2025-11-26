package app.notedrop.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.notedrop.android.domain.model.ObsidianVaultConfig
import app.notedrop.android.domain.model.ProviderConfig

/**
 * Screen for viewing and configuring Obsidian vault settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultConfigurationScreen(
    vaultConfig: ObsidianVaultConfig,
    vault: app.notedrop.android.domain.model.Vault,
    isDefault: Boolean,
    onNavigateBack: () -> Unit,
    onSaveConfig: (ObsidianVaultConfig) -> Unit,
    onSetDefault: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSaveConfig(vaultConfig) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
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
            // Vault Info
            item {
                VaultInfoSection(vaultConfig)
            }

            // Set as Default button
            if (!isDefault) {
                item {
                    Button(
                        onClick = onSetDefault,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Default Vault")
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "This is your default vault",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Daily Notes Configuration
            item {
                Text(
                    text = "Daily Notes",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                vaultConfig.dailyNotes?.let { dailyNotes ->
                    DailyNotesConfigSection(dailyNotes)
                } ?: run {
                    NoConfigCard("Daily notes not configured in this vault")
                }
            }

            // App Configuration
            item {
                Text(
                    text = "Vault Settings",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                vaultConfig.app?.let { app ->
                    AppConfigSection(app)
                } ?: run {
                    NoConfigCard("App settings not configured")
                }
            }

            // Active App Configuration (what the app is actually using)
            item {
                Text(
                    text = "Active App Configuration",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Text(
                    text = "These are the values currently being used by NoteDrop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                (vault.providerConfig as? ProviderConfig.ObsidianConfig)?.let { config ->
                    ActiveConfigSection(config)
                } ?: run {
                    NoConfigCard("Provider configuration not available")
                }
            }
        }
    }
}

@Composable
private fun VaultInfoSection(config: ObsidianVaultConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column {
                Text(
                    text = config.vaultName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Obsidian Vault",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DailyNotesConfigSection(config: app.notedrop.android.domain.model.DailyNotesConfig) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Folder
            config.folder?.let { folder ->
                ConfigItem(
                    icon = Icons.Default.Folder,
                    label = "Daily Notes Folder",
                    value = folder
                )
            }

            // Format
            config.format?.let { format ->
                ConfigItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Date Format",
                    value = format
                )
            }

            // Template
            config.template?.let { template ->
                ConfigItem(
                    icon = Icons.Default.Description,
                    label = "Template",
                    value = template
                )
            }

            // Autorun
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Auto-create Daily Note",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.autorun) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.autorun) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.autorun) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppConfigSection(config: app.notedrop.android.domain.model.AppConfig) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Attachment folder
            config.attachmentFolderPath?.let { path ->
                ConfigItem(
                    icon = Icons.Default.AttachFile,
                    label = "Attachments Folder",
                    value = path
                )
            }

            // Prompt delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Confirm Before Delete",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.promptDelete) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.promptDelete) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.promptDelete) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ActiveConfigSection(config: ProviderConfig.ObsidianConfig) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vault Path
            ConfigItem(
                icon = Icons.Default.Folder,
                label = "Vault Path",
                value = config.vaultPath
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Storage Paths Section
            Text(
                text = "Storage Paths",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            config.dailyNotesPath?.let { path ->
                ConfigItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Daily Notes Path",
                    value = path
                )
            } ?: ConfigItem(
                icon = Icons.Default.CalendarToday,
                label = "Daily Notes Path",
                value = "Not set (vault root)"
            )

            config.dailyNotesFormat?.let { format ->
                ConfigItem(
                    icon = Icons.Default.DateRange,
                    label = "Daily Notes Format",
                    value = format
                )
            } ?: ConfigItem(
                icon = Icons.Default.DateRange,
                label = "Daily Notes Format",
                value = "Not set (default: YYYY-MM-DD)"
            )

            config.attachmentsPath?.let { path ->
                ConfigItem(
                    icon = Icons.Default.AttachFile,
                    label = "Attachments Path (Active)",
                    value = path
                )
            } ?: ConfigItem(
                icon = Icons.Default.AttachFile,
                label = "Attachments Path (Active)",
                value = "Not set (default: attachments)"
            )

            config.templatePath?.let { path ->
                ConfigItem(
                    icon = Icons.Default.Description,
                    label = "Template Path",
                    value = path
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Markdown Settings Section
            Text(
                text = "Markdown Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Use Front Matter",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.useFrontMatter) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.useFrontMatter) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.useFrontMatter) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Preserve Obsidian Links",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.preserveObsidianLinks) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.preserveObsidianLinks) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.preserveObsidianLinks) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            config.frontMatterTemplate?.let { template ->
                ConfigItem(
                    icon = Icons.Default.Description,
                    label = "Front Matter Template",
                    value = template
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Sync Settings Section
            Text(
                text = "Sync Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            ConfigItem(
                icon = Icons.Default.Sync,
                label = "Sync Mode",
                value = config.syncMode.toString()
            )

            ConfigItem(
                icon = Icons.Default.Warning,
                label = "Conflict Strategy",
                value = config.conflictStrategy.toString().replace("_", " ")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Watch for Changes",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.watchForChanges) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.watchForChanges) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.watchForChanges) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ConfigItem(
                icon = Icons.Default.Timer,
                label = "Auto Sync Interval",
                value = "${config.autoSyncIntervalMinutes} minutes"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Features Section
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Backlinks",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.enableBacklinks) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.enableBacklinks) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.enableBacklinks) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Template Variables",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (config.enableTemplateVariables) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (config.enableTemplateVariables) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (config.enableTemplateVariables) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NoConfigCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
