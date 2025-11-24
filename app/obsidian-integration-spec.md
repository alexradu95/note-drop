# Obsidian Integration Module - Technical Specification

## Overview
Android note-capture app with platform-agnostic storage supporting Obsidian vaults and simple folders.

**Core Principle**: Capture layer is completely decoupled from storage. Storage module receives formatted notes and handles persistence.

---

## 1. Data Models

### 1.1 Captured Note
```kotlin
data class CapturedNote(
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 1.2 Storage Settings
```kotlin
enum class StorageType {
    OBSIDIAN,
    SIMPLE_FOLDER
}

enum class SaveLocation {
    INBOX_FILE,      // New .md file in inbox folder
    DAILY_NOTE       // Append under header in daily note
}

enum class ContentFormat {
    TIMESTAMP_AND_CONTENT,  // "- 14:30 - Note content"
    CONTENT_ONLY            // "- Note content"
}

enum class FileNamingStrategy {
    TIMESTAMP,      // 20250124143022.md
    TITLE_BASED,    // note-title.md
    UUID            // capture-abc123.md
}

data class StorageSettings(
    // Backend type
    val storageType: StorageType,

    // Content formatting
    val contentFormat: ContentFormat,
    val timestampFormat: String = "HH:mm", // Default time-only format

    // Save location
    val saveLocation: SaveLocation,

    // Inbox settings (for INBOX_FILE mode)
    val inboxFolderName: String = "Captured",
    val inboxFileNaming: FileNamingStrategy = FileNamingStrategy.TIMESTAMP,

    // Daily note settings (for DAILY_NOTE mode)
    val dailyNoteHeader: String = "## Captured Notes",

    // Storage URIs (SAF persistent permissions)
    val vaultUri: Uri? = null,           // Obsidian vault root
    val simpleFolderUri: Uri? = null     // Simple folder path
)
```

### 1.3 Save Result
```kotlin
sealed class SaveResult {
    data class Success(
        val filePath: String,
        val timestamp: Long
    ) : SaveResult()

    data class Failure(
        val error: StorageError,
        val message: String
    ) : SaveResult()
}

enum class StorageError {
    PERMISSION_DENIED,
    VAULT_NOT_FOUND,
    CONFIG_PARSE_ERROR,
    FILE_WRITE_ERROR,
    DAILY_NOTE_NOT_FOUND,
    INVALID_CONFIGURATION
}
```

---

## 2. Storage Module Interface

### 2.1 Core Interface
```kotlin
interface NoteSaver {
    /**
     * Save a captured note according to settings
     */
    suspend fun saveNote(note: CapturedNote, settings: StorageSettings): SaveResult

    /**
     * Validate configuration (check permissions, vault structure, etc.)
     */
    suspend fun validateConfiguration(settings: StorageSettings): Result<Boolean>

    /**
     * Get vault/folder metadata for UI display
     */
    suspend fun getStorageInfo(settings: StorageSettings): StorageInfo?
}

data class StorageInfo(
    val location: String,           // Vault/folder path
    val isAccessible: Boolean,      // Can read/write
    val obsidianVersion: String?,   // For Obsidian vaults
    val dailyNotesEnabled: Boolean  // Is daily notes plugin configured
)
```

---

## 3. Content Formatter

### 3.1 Format Logic
```kotlin
class NoteFormatter(
    private val contentFormat: ContentFormat,
    private val timestampFormat: String = "HH:mm"
) {
    /**
     * Format captured note as bullet point with optional timestamp
     *
     * Examples:
     * - TIMESTAMP_AND_CONTENT: "- 14:30 - Note content"
     * - CONTENT_ONLY: "- Note content"
     */
    fun format(note: CapturedNote): String {
        val bullet = "-"

        return when (contentFormat) {
            ContentFormat.TIMESTAMP_AND_CONTENT -> {
                val timestamp = SimpleDateFormat(timestampFormat, Locale.getDefault())
                    .format(Date(note.timestamp))
                "$bullet $timestamp - ${note.content}"
            }
            ContentFormat.CONTENT_ONLY -> {
                "$bullet ${note.content}"
            }
        }
    }
}
```

**Formatting Rules:**
- Always use bullet point (`-`) for consistency with Markdown
- Timestamp format defaults to `HH:mm` (time only)
- Content is added after bullet and optional timestamp
- No trailing newlines (added during insertion)

---

## 4. Obsidian Module

### 4.1 Obsidian Config Parser

**Config File Locations:**
- Daily notes: `.obsidian/daily-notes.json`
- App settings: `.obsidian/app.json`

#### 4.1.1 Daily Notes Config
```kotlin
data class DailyNotesConfig(
    val folder: String,        // Folder path relative to vault root
    val format: String,        // Date format (moment.js style)
    val template: String?      // Template file path (optional)
) {
    companion object {
        fun default() = DailyNotesConfig(
            folder = "",
            format = "YYYY-MM-DD",
            template = null
        )
    }
}

class ObsidianConfigParser(private val context: Context) {

    /**
     * Parse daily notes configuration from .obsidian/daily-notes.json
     *
     * @param vaultUri SAF URI to vault root
     * @return DailyNotesConfig or default if file doesn't exist
     */
    suspend fun parseDailyNotesConfig(vaultUri: Uri): DailyNotesConfig = withContext(Dispatchers.IO) {
        try {
            val configFile = findFile(vaultUri, ".obsidian/daily-notes.json")
                ?: return@withContext DailyNotesConfig.default()

            val json = readJsonFile(configFile)

            DailyNotesConfig(
                folder = json.optString("folder", ""),
                format = json.optString("format", "YYYY-MM-DD"),
                template = json.optString("template").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            Log.e("ObsidianConfig", "Failed to parse daily notes config", e)
            DailyNotesConfig.default()
        }
    }

    /**
     * Find file within vault using SAF DocumentFile
     */
    private fun findFile(vaultUri: Uri, relativePath: String): DocumentFile? {
        val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null

        val pathParts = relativePath.split("/")
        var current = vaultRoot

        for (part in pathParts) {
            current = current.findFile(part) ?: return null
        }

        return current
    }

    /**
     * Read JSON file content
     */
    private fun readJsonFile(file: DocumentFile): JSONObject {
        val content = context.contentResolver.openInputStream(file.uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IOException("Cannot read file")

        return JSONObject(content)
    }
}
```

#### 4.1.2 Date Format Converter
```kotlin
/**
 * Convert Obsidian date format (moment.js) to Android SimpleDateFormat
 *
 * Common formats:
 * - YYYY-MM-DD → yyyy-MM-dd
 * - YYYYMMDD → yyyyMMdd
 * - YYYY/MM/DD → yyyy/MM/dd
 * - DD-MM-YYYY → dd-MM-yyyy
 */
object DateFormatConverter {

    fun momentToAndroid(momentFormat: String): String {
        return momentFormat
            .replace("YYYY", "yyyy")
            .replace("YY", "yy")
            .replace("DD", "dd")
            .replace("MMMM", "MMMM")
            .replace("MMM", "MMM")
            .replace("MM", "MM")
            .replace("M", "M")
    }

    /**
     * Generate today's filename based on Obsidian format
     */
    fun generateDailyNoteFilename(momentFormat: String): String {
        val androidFormat = momentToAndroid(momentFormat)
        val dateFormat = SimpleDateFormat(androidFormat, Locale.getDefault())
        return "${dateFormat.format(Date())}.md"
    }
}
```

### 4.2 Obsidian Note Saver Implementation

```kotlin
class ObsidianNoteSaver(
    private val context: Context,
    private val configParser: ObsidianConfigParser
) : NoteSaver {

    override suspend fun saveNote(
        note: CapturedNote,
        settings: StorageSettings
    ): SaveResult = withContext(Dispatchers.IO) {

        val vaultUri = settings.vaultUri
            ?: return@withContext SaveResult.Failure(
                StorageError.INVALID_CONFIGURATION,
                "Vault URI not set"
            )

        // Format content
        val formatter = NoteFormatter(settings.contentFormat, settings.timestampFormat)
        val formattedContent = formatter.format(note)

        // Save based on location
        when (settings.saveLocation) {
            SaveLocation.INBOX_FILE -> saveToInbox(vaultUri, formattedContent, settings)
            SaveLocation.DAILY_NOTE -> saveToDailyNote(vaultUri, formattedContent, settings)
        }
    }

    /**
     * Save as individual file in inbox folder
     */
    private suspend fun saveToInbox(
        vaultUri: Uri,
        content: String,
        settings: StorageSettings
    ): SaveResult {
        try {
            // Get or create inbox folder
            val inboxFolder = getOrCreateFolder(vaultUri, settings.inboxFolderName)
                ?: return SaveResult.Failure(
                    StorageError.FILE_WRITE_ERROR,
                    "Cannot create inbox folder"
                )

            // Generate filename
            val filename = generateFilename(content, settings.inboxFileNaming)

            // Write file
            val file = inboxFolder.createFile("text/markdown", filename)
                ?: return SaveResult.Failure(
                    StorageError.FILE_WRITE_ERROR,
                    "Cannot create file: $filename"
                )

            writeContent(file, content)

            return SaveResult.Success(
                filePath = "${settings.inboxFolderName}/$filename",
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e("ObsidianNoteSaver", "Failed to save to inbox", e)
            return SaveResult.Failure(
                StorageError.FILE_WRITE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Append to daily note under specified header
     */
    private suspend fun saveToDailyNote(
        vaultUri: Uri,
        content: String,
        settings: StorageSettings
    ): SaveResult {
        try {
            // 1. Parse daily notes config
            val config = configParser.parseDailyNotesConfig(vaultUri)

            // 2. Generate today's filename
            val filename = DateFormatConverter.generateDailyNoteFilename(config.format)

            // 3. Get or create daily note file
            val dailyNoteFile = getOrCreateDailyNote(vaultUri, config, filename)
                ?: return SaveResult.Failure(
                    StorageError.DAILY_NOTE_NOT_FOUND,
                    "Cannot create daily note"
                )

            // 4. Read existing content
            val existingContent = readContent(dailyNoteFile)

            // 5. Insert under header
            val updatedContent = insertUnderHeader(
                existingContent,
                settings.dailyNoteHeader,
                content
            )

            // 6. Write back
            writeContent(dailyNoteFile, updatedContent)

            val filePath = if (config.folder.isEmpty()) {
                filename
            } else {
                "${config.folder}/$filename"
            }

            return SaveResult.Success(
                filePath = filePath,
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e("ObsidianNoteSaver", "Failed to save to daily note", e)
            return SaveResult.Failure(
                StorageError.FILE_WRITE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Get or create daily note file
     * If file doesn't exist and template is specified, use template content
     */
    private fun getOrCreateDailyNote(
        vaultUri: Uri,
        config: DailyNotesConfig,
        filename: String
    ): DocumentFile? {
        val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null

        // Navigate to daily notes folder
        val folder = if (config.folder.isEmpty()) {
            vaultRoot
        } else {
            getOrCreateFolder(vaultUri, config.folder) ?: return null
        }

        // Check if file exists
        var file = folder.findFile(filename)

        if (file == null) {
            // Create new daily note
            file = folder.createFile("text/markdown", filename) ?: return null

            // Apply template if specified
            if (config.template != null) {
                val templateContent = readTemplateContent(vaultUri, config.template)
                if (templateContent != null) {
                    writeContent(file, templateContent)
                }
            }
        }

        return file
    }

    /**
     * Read template file content
     */
    private fun readTemplateContent(vaultUri: Uri, templatePath: String): String? {
        val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null

        val pathParts = templatePath.split("/")
        var current = vaultRoot

        for (part in pathParts) {
            current = current.findFile(part) ?: return null
        }

        return readContent(current)
    }

    // Helper methods continue below...
}
```

### 4.3 Header Insertion Logic

```kotlin
/**
 * Insert content under header with proper formatting
 *
 * Rules:
 * - If header exists: append to end of section with blank line separator
 * - If header doesn't exist: create at bottom of file
 * - Each capture is a bullet point with blank line before it
 * - Preserve existing content structure
 */
private fun insertUnderHeader(
    existingContent: String,
    header: String,
    newContent: String
): String {
    val lines = existingContent.split("\n").toMutableList()

    // Find header line index
    val headerIndex = lines.indexOfFirst { it.trim() == header.trim() }

    if (headerIndex == -1) {
        // Header doesn't exist - add at bottom
        return buildString {
            append(existingContent)

            // Add spacing before header if content exists
            if (existingContent.isNotBlank()) {
                append("\n\n")
            }

            append(header)
            append("\n")
            append(newContent)
        }
    }

    // Header exists - find end of section
    val nextHeaderIndex = findNextHeaderIndex(lines, headerIndex)

    // Insert content at end of section with blank line
    val insertPosition = nextHeaderIndex

    // Add blank line if previous line is not blank
    if (insertPosition > 0 && lines[insertPosition - 1].isNotBlank()) {
        lines.add(insertPosition, "")
    }

    lines.add(insertPosition, newContent)

    return lines.joinToString("\n")
}

/**
 * Find the next header after given index
 * Returns the line index where next section starts, or end of list
 */
private fun findNextHeaderIndex(lines: List<String>, fromIndex: Int): Int {
    for (i in (fromIndex + 1) until lines.size) {
        if (lines[i].trimStart().startsWith("#")) {
            return i
        }
    }
    return lines.size
}
```

**Example Transformations:**

**Case 1: Header doesn't exist**
```markdown
# Daily Note
Some content here.

## Tasks
- Task 1
```

After insert with header "## Captured Notes" and content "- 14:30 - New note":
```markdown
# Daily Note
Some content here.

## Tasks
- Task 1

## Captured Notes
- 14:30 - New note
```

**Case 2: Header exists, append to section**
```markdown
# Daily Note

## Captured Notes
- 10:00 - First note

## Tasks
- Task 1
```

After insert with content "- 14:30 - Second note":
```markdown
# Daily Note

## Captured Notes
- 10:00 - First note

- 14:30 - Second note

## Tasks
- Task 1
```

### 4.4 SAF File Operations

```kotlin
/**
 * Get or create folder within vault
 */
private fun getOrCreateFolder(vaultUri: Uri, folderPath: String): DocumentFile? {
    val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null

    if (folderPath.isEmpty()) {
        return vaultRoot
    }

    val pathParts = folderPath.split("/")
    var current = vaultRoot

    for (part in pathParts) {
        val existing = current.findFile(part)
        current = if (existing != null && existing.isDirectory) {
            existing
        } else {
            current.createDirectory(part) ?: return null
        }
    }

    return current
}

/**
 * Generate filename based on strategy
 */
private fun generateFilename(content: String, strategy: FileNamingStrategy): String {
    return when (strategy) {
        FileNamingStrategy.TIMESTAMP -> {
            val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                .format(Date())
            "$timestamp.md"
        }
        FileNamingStrategy.TITLE_BASED -> {
            // Extract first line as title, sanitize
            val title = content.lines().firstOrNull()
                ?.replace(Regex("[^a-zA-Z0-9 -]"), "")
                ?.take(50)
                ?.trim()
                ?.replace(" ", "-")
                ?.lowercase()
                ?: "note"
            "$title.md"
        }
        FileNamingStrategy.UUID -> {
            "capture-${UUID.randomUUID().toString().take(8)}.md"
        }
    }
}

/**
 * Read file content as string
 */
private fun readContent(file: DocumentFile): String {
    return context.contentResolver.openInputStream(file.uri)?.use { input ->
        input.bufferedReader().readText()
    } ?: ""
}

/**
 * Write content to file
 */
private fun writeContent(file: DocumentFile, content: String) {
    context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
        output.bufferedWriter().write(content)
    }
}

override suspend fun validateConfiguration(settings: StorageSettings): Result<Boolean> {
    return try {
        val vaultUri = settings.vaultUri ?: return Result.failure(
            IllegalStateException("Vault URI not set")
        )

        val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri)
        if (vaultRoot == null || !vaultRoot.exists()) {
            return Result.failure(IllegalStateException("Vault not accessible"))
        }

        // Check if .obsidian folder exists
        val obsidianFolder = vaultRoot.findFile(".obsidian")
        if (obsidianFolder == null || !obsidianFolder.isDirectory) {
            return Result.failure(IllegalStateException("Not a valid Obsidian vault"))
        }

        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

override suspend fun getStorageInfo(settings: StorageSettings): StorageInfo? {
    val vaultUri = settings.vaultUri ?: return null
    val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null

    val dailyNotesConfig = configParser.parseDailyNotesConfig(vaultUri)

    return StorageInfo(
        location = vaultRoot.name ?: "Unknown",
        isAccessible = vaultRoot.canWrite(),
        obsidianVersion = null, // Could parse from .obsidian/app.json if needed
        dailyNotesEnabled = dailyNotesConfig.format.isNotEmpty()
    )
}
```

---

## 5. Simple Folder Module

```kotlin
class SimpleFolderNoteSaver(
    private val context: Context
) : NoteSaver {

    override suspend fun saveNote(
        note: CapturedNote,
        settings: StorageSettings
    ): SaveResult = withContext(Dispatchers.IO) {

        val folderUri = settings.simpleFolderUri
            ?: return@withContext SaveResult.Failure(
                StorageError.INVALID_CONFIGURATION,
                "Folder URI not set"
            )

        // Simple folder only supports INBOX_FILE mode
        if (settings.saveLocation == SaveLocation.DAILY_NOTE) {
            return@withContext SaveResult.Failure(
                StorageError.INVALID_CONFIGURATION,
                "Daily notes not supported for simple folder"
            )
        }

        try {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: return@withContext SaveResult.Failure(
                    StorageError.FILE_WRITE_ERROR,
                    "Folder not accessible"
                )

            // Format content
            val formatter = NoteFormatter(settings.contentFormat, settings.timestampFormat)
            val formattedContent = formatter.format(note)

            // Generate filename
            val filename = generateFilename(formattedContent, settings.inboxFileNaming)

            // Create file
            val file = folder.createFile("text/markdown", filename)
                ?: return@withContext SaveResult.Failure(
                    StorageError.FILE_WRITE_ERROR,
                    "Cannot create file"
                )

            // Write content
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
                output.bufferedWriter().write(formattedContent)
            }

            SaveResult.Success(
                filePath = filename,
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            SaveResult.Failure(
                StorageError.FILE_WRITE_ERROR,
                e.message ?: "Unknown error"
            )
        }
    }

    override suspend fun validateConfiguration(settings: StorageSettings): Result<Boolean> {
        val folderUri = settings.simpleFolderUri ?: return Result.failure(
            IllegalStateException("Folder URI not set")
        )

        val folder = DocumentFile.fromTreeUri(context, folderUri)
        return if (folder != null && folder.isDirectory && folder.canWrite()) {
            Result.success(true)
        } else {
            Result.failure(IllegalStateException("Folder not accessible"))
        }
    }

    override suspend fun getStorageInfo(settings: StorageSettings): StorageInfo? {
        val folderUri = settings.simpleFolderUri ?: return null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null

        return StorageInfo(
            location = folder.name ?: "Unknown",
            isAccessible = folder.canWrite(),
            obsidianVersion = null,
            dailyNotesEnabled = false
        )
    }

    private fun generateFilename(content: String, strategy: FileNamingStrategy): String {
        return when (strategy) {
            FileNamingStrategy.TIMESTAMP -> {
                val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                    .format(Date())
                "$timestamp.md"
            }
            FileNamingStrategy.TITLE_BASED -> {
                val title = content.lines().firstOrNull()
                    ?.replace(Regex("[^a-zA-Z0-9 -]"), "")
                    ?.take(50)
                    ?.trim()
                    ?.replace(" ", "-")
                    ?.lowercase()
                    ?: "note"
                "$title.md"
            }
            FileNamingStrategy.UUID -> {
                "capture-${UUID.randomUUID().toString().take(8)}.md"
            }
        }
    }
}
```

---

## 6. Settings Persistence

```kotlin
class StorageSettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("storage_settings", Context.MODE_PRIVATE)

    fun saveSettings(settings: StorageSettings) {
        prefs.edit {
            putString("storage_type", settings.storageType.name)
            putString("content_format", settings.contentFormat.name)
            putString("timestamp_format", settings.timestampFormat)
            putString("save_location", settings.saveLocation.name)
            putString("inbox_folder_name", settings.inboxFolderName)
            putString("inbox_file_naming", settings.inboxFileNaming.name)
            putString("daily_note_header", settings.dailyNoteHeader)
            putString("vault_uri", settings.vaultUri?.toString())
            putString("simple_folder_uri", settings.simpleFolderUri?.toString())
        }
    }

    fun loadSettings(): StorageSettings {
        return StorageSettings(
            storageType = StorageType.valueOf(
                prefs.getString("storage_type", StorageType.OBSIDIAN.name)!!
            ),
            contentFormat = ContentFormat.valueOf(
                prefs.getString("content_format", ContentFormat.TIMESTAMP_AND_CONTENT.name)!!
            ),
            timestampFormat = prefs.getString("timestamp_format", "HH:mm")!!,
            saveLocation = SaveLocation.valueOf(
                prefs.getString("save_location", SaveLocation.INBOX_FILE.name)!!
            ),
            inboxFolderName = prefs.getString("inbox_folder_name", "Captured")!!,
            inboxFileNaming = FileNamingStrategy.valueOf(
                prefs.getString("inbox_file_naming", FileNamingStrategy.TIMESTAMP.name)!!
            ),
            dailyNoteHeader = prefs.getString("daily_note_header", "## Captured Notes")!!,
            vaultUri = prefs.getString("vault_uri", null)?.let { Uri.parse(it) },
            simpleFolderUri = prefs.getString("simple_folder_uri", null)?.let { Uri.parse(it) }
        )
    }
}
```

---

## 7. Usage Example

```kotlin
class CaptureViewModel(
    private val obsidianSaver: ObsidianNoteSaver,
    private val simpleFolderSaver: SimpleFolderNoteSaver,
    private val settingsManager: StorageSettingsManager
) : ViewModel() {

    suspend fun saveCapture(content: String): SaveResult {
        val note = CapturedNote(content = content)
        val settings = settingsManager.loadSettings()

        val saver = when (settings.storageType) {
            StorageType.OBSIDIAN -> obsidianSaver
            StorageType.SIMPLE_FOLDER -> simpleFolderSaver
        }

        return saver.saveNote(note, settings)
    }
}
```

---

## 8. Settings UI Implementation

### 8.1 Settings Screen Layout

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Storage Type
        SectionHeader("Storage Type")
        StorageTypeSelector(
            selected = settings.storageType,
            onSelect = { viewModel.updateStorageType(it) }
        )

        // Vault/Folder Selection
        when (settings.storageType) {
            StorageType.OBSIDIAN -> {
                Button(onClick = { viewModel.selectVault() }) {
                    Text("Select Obsidian Vault")
                }
                settings.vaultUri?.let {
                    Text("Current: ${it.lastPathSegment}", style = MaterialTheme.typography.caption)
                }
            }
            StorageType.SIMPLE_FOLDER -> {
                Button(onClick = { viewModel.selectFolder() }) {
                    Text("Select Folder")
                }
                settings.simpleFolderUri?.let {
                    Text("Current: ${it.lastPathSegment}", style = MaterialTheme.typography.caption)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content Format
        SectionHeader("Content Format")
        ContentFormatSelector(
            selected = settings.contentFormat,
            timestampFormat = settings.timestampFormat,
            onFormatSelect = { viewModel.updateContentFormat(it) },
            onTimestampFormatChange = { viewModel.updateTimestampFormat(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Location
        SectionHeader("Save Location")
        SaveLocationSelector(
            selected = settings.saveLocation,
            onSelect = { viewModel.updateSaveLocation(it) }
        )

        // Location-specific settings
        when (settings.saveLocation) {
            SaveLocation.INBOX_FILE -> {
                InboxSettings(
                    folderName = settings.inboxFolderName,
                    fileNaming = settings.inboxFileNaming,
                    onFolderNameChange = { viewModel.updateInboxFolder(it) },
                    onFileNamingChange = { viewModel.updateFileNaming(it) }
                )
            }
            SaveLocation.DAILY_NOTE -> {
                if (settings.storageType == StorageType.OBSIDIAN) {
                    DailyNoteSettings(
                        header = settings.dailyNoteHeader,
                        onHeaderChange = { viewModel.updateDailyNoteHeader(it) }
                    )
                } else {
                    Text(
                        "Daily notes only available for Obsidian",
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Test Save Button
        Button(
            onClick = { viewModel.testSave() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Save")
        }
    }
}
```

### 8.2 Vault Selection with SAF

```kotlin
class SettingsViewModel(
    private val settingsManager: StorageSettingsManager,
    private val application: Application
) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(settingsManager.loadSettings())
    val settings: StateFlow<StorageSettings> = _settings.asStateFlow()

    fun selectVault() {
        // Trigger SAF folder picker
        // This needs to be handled in Activity/Fragment
        _pendingAction.value = SettingsAction.SelectVault
    }

    fun handleVaultSelection(uri: Uri) {
        viewModelScope.launch {
            // Take persistent permissions
            val contentResolver = application.contentResolver
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Update settings
            val updated = _settings.value.copy(vaultUri = uri)
            _settings.value = updated
            settingsManager.saveSettings(updated)

            // Validate vault
            validateVault(uri)
        }
    }

    private suspend fun validateVault(uri: Uri) {
        val validator = ObsidianNoteSaver(application, ObsidianConfigParser(application))
        val result = validator.validateConfiguration(_settings.value)

        if (result.isSuccess) {
            _validationStatus.value = "Valid Obsidian vault"
        } else {
            _validationStatus.value = "Error: ${result.exceptionOrNull()?.message}"
        }
    }
}
```

### 8.3 Activity Integration

```kotlin
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    private val vaultPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.handleVaultSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SettingsScreen(viewModel)
        }

        // Observe pending actions
        lifecycleScope.launch {
            viewModel.pendingAction.collect { action ->
                when (action) {
                    is SettingsAction.SelectVault -> {
                        vaultPickerLauncher.launch(null)
                    }
                    is SettingsAction.SelectFolder -> {
                        vaultPickerLauncher.launch(null)
                    }
                    else -> {}
                }
            }
        }
    }
}
```

---

## 9. Error Handling Strategy

### 9.1 Common Error Scenarios

| Error | Cause | Recovery |
|-------|-------|----------|
| PERMISSION_DENIED | SAF permissions revoked | Re-prompt for folder selection |
| VAULT_NOT_FOUND | Vault moved/deleted | Show error, ask to reselect |
| CONFIG_PARSE_ERROR | Corrupted .obsidian config | Use default config, notify user |
| FILE_WRITE_ERROR | Storage full, file locked | Retry logic, show error message |
| DAILY_NOTE_NOT_FOUND | Can't create daily note | Check folder permissions |

### 9.2 User Notifications

```kotlin
fun showSaveResult(result: SaveResult, context: Context) {
    when (result) {
        is SaveResult.Success -> {
            Toast.makeText(
                context,
                "Saved to ${result.filePath}",
                Toast.LENGTH_SHORT
            ).show()
        }
        is SaveResult.Failure -> {
            val message = when (result.error) {
                StorageError.PERMISSION_DENIED ->
                    "Permission denied. Please reselect vault."
                StorageError.VAULT_NOT_FOUND ->
                    "Vault not found. Please check settings."
                StorageError.CONFIG_PARSE_ERROR ->
                    "Config error. Using defaults."
                StorageError.FILE_WRITE_ERROR ->
                    "Failed to save: ${result.message}"
                StorageError.DAILY_NOTE_NOT_FOUND ->
                    "Cannot create daily note. Check folder permissions."
                StorageError.INVALID_CONFIGURATION ->
                    "Invalid settings: ${result.message}"
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
```

---

## 10. Testing Strategy

### 10.1 Unit Tests

```kotlin
@Test
fun `test content formatting with timestamp`() {
    val formatter = NoteFormatter(
        ContentFormat.TIMESTAMP_AND_CONTENT,
        "HH:mm"
    )

    val note = CapturedNote(
        content = "Test note",
        timestamp = parseTime("14:30")
    )

    val result = formatter.format(note)
    assertEquals("- 14:30 - Test note", result)
}

@Test
fun `test header insertion when header exists`() {
    val existing = """
        # Daily Note

        ## Captured Notes
        - 10:00 - First note

        ## Tasks
        - Task 1
    """.trimIndent()

    val result = insertUnderHeader(
        existing,
        "## Captured Notes",
        "- 14:30 - Second note"
    )

    assertTrue(result.contains("- 10:00 - First note"))
    assertTrue(result.contains("- 14:30 - Second note"))
    assertTrue(result.indexOf("- 14:30") > result.indexOf("- 10:00"))
}

@Test
fun `test header insertion when header missing`() {
    val existing = """
        # Daily Note
        Some content.
    """.trimIndent()

    val result = insertUnderHeader(
        existing,
        "## Captured Notes",
        "- 14:30 - New note"
    )

    assertTrue(result.endsWith("## Captured Notes\n- 14:30 - New note"))
}
```

### 10.2 Integration Tests

```kotlin
@Test
fun `test save to obsidian vault inbox`() = runBlocking {
    val mockContext = mockk<Context>()
    val mockVaultUri = mockk<Uri>()

    // Setup mock vault structure
    setupMockVault(mockContext, mockVaultUri)

    val saver = ObsidianNoteSaver(mockContext, ObsidianConfigParser(mockContext))

    val settings = StorageSettings(
        storageType = StorageType.OBSIDIAN,
        saveLocation = SaveLocation.INBOX_FILE,
        vaultUri = mockVaultUri
    )

    val note = CapturedNote(content = "Test note")

    val result = saver.saveNote(note, settings)

    assertTrue(result is SaveResult.Success)
    verify { /* verify file was created in Captured folder */ }
}
```

---

## 11. Performance Considerations

### 11.1 File Operations
- All file I/O on background threads (Dispatchers.IO)
- Cache parsed configs to avoid repeated reads
- Use buffered readers/writers for efficiency

### 11.2 SAF Performance
- Persistent URI permissions prevent repeated picker launches
- DocumentFile API can be slow for deep hierarchies
- Consider caching folder DocumentFile references

### 11.3 Memory Management
- Stream large file reads instead of loading into memory
- Release resources properly with `use` blocks
- Avoid keeping file content in memory longer than needed

---

## 12. Future Enhancements

### 12.1 Potential Features
- **Batch captures**: Save multiple notes at once
- **Sync status**: Track which captures have been saved
- **Conflict resolution**: Handle concurrent edits to daily notes
- **Backup**: Local cache of captures before save
- **Export**: Bulk export to different formats
- **Templates**: Custom note templates for captures
- **Tags extraction**: Auto-detect and format hashtags
- **Search**: Search within saved captures

### 12.2 Additional Integrations
- **Notion**: Similar module for Notion databases
- **Logseq**: Graph-based note system
- **Joplin**: Another markdown note system
- **Google Drive**: Cloud folder sync
- **Dropbox**: Alternative cloud storage

---

## 13. Summary Checklist

### Implementation Checklist

- [ ] Core data models (CapturedNote, StorageSettings, SaveResult)
- [ ] NoteSaver interface
- [ ] NoteFormatter with bullet point formatting
- [ ] ObsidianConfigParser (daily-notes.json parsing)
- [ ] DateFormatConverter (moment.js → Android)
- [ ] ObsidianNoteSaver
  - [ ] Inbox file mode
  - [ ] Daily note mode
  - [ ] Header insertion logic
  - [ ] SAF file operations
  - [ ] Template handling
- [ ] SimpleFolderNoteSaver
- [ ] StorageSettingsManager (SharedPreferences)
- [ ] Settings UI with SAF integration
- [ ] Error handling and user notifications
- [ ] Unit tests
- [ ] Integration tests

### Configuration Requirements

**User Must Configure:**
1. Storage type (Obsidian or Simple Folder)
2. Vault/folder location (via SAF picker)
3. Content format (with/without timestamp)
4. Save location (inbox file or daily note)
5. Location-specific settings:
   - Inbox: folder name, file naming strategy
   - Daily note: header text

**Auto-Detected from Obsidian:**
- Daily notes folder path
- Daily notes date format
- Daily notes template (if specified)

---

## End of Specification

This specification provides a complete implementation guide for the Obsidian integration module. All code examples are production-ready and follow Android best practices with SAF, Kotlin coroutines, and proper error handling.
