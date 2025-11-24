package app.notedrop.android.domain.model

/**
 * Obsidian vault configuration read from .obsidian folder
 */
data class ObsidianVaultConfig(
    val dailyNotes: DailyNotesConfig? = null,
    val app: AppConfig? = null,
    val vaultName: String = ""
)

/**
 * Daily notes configuration from daily-notes.json
 */
data class DailyNotesConfig(
    val folder: String? = null,
    val format: String? = null,
    val autorun: Boolean = false,
    val template: String? = null
)

/**
 * App configuration from app.json
 */
data class AppConfig(
    val promptDelete: Boolean = false,
    val attachmentFolderPath: String? = null
)
