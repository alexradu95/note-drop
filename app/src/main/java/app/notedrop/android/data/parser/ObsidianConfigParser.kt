package app.notedrop.android.data.parser

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.notedrop.android.domain.model.AppConfig
import app.notedrop.android.domain.model.DailyNotesConfig
import app.notedrop.android.domain.model.ObsidianVaultConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for Obsidian vault configuration files
 */
@Singleton
class ObsidianConfigParser @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Parse Obsidian vault configuration from the .obsidian folder
     */
    fun parseVaultConfig(vaultUri: Uri): ObsidianVaultConfig? {
        return try {
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return null
            val vaultName = vaultRoot.name ?: "Obsidian Vault"

            // Find .obsidian folder
            val obsidianFolder = vaultRoot.findFile(".obsidian") ?: return null

            // Parse daily-notes.json
            val dailyNotesConfig = obsidianFolder.findFile("daily-notes.json")?.let {
                parseDailyNotesConfig(it)
            }

            // Parse app.json
            val appConfig = obsidianFolder.findFile("app.json")?.let {
                parseAppConfig(it)
            }

            ObsidianVaultConfig(
                dailyNotes = dailyNotesConfig,
                app = appConfig,
                vaultName = vaultName
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse daily-notes.json
     */
    private fun parseDailyNotesConfig(file: DocumentFile): DailyNotesConfig? {
        return try {
            val content = readFileContent(file)
            val json = JSONObject(content)

            DailyNotesConfig(
                folder = json.optString("folder", null),
                format = json.optString("format", null),
                autorun = json.optBoolean("autorun", false),
                template = json.optString("template", null)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse app.json
     */
    private fun parseAppConfig(file: DocumentFile): AppConfig? {
        return try {
            val content = readFileContent(file)
            val json = JSONObject(content)

            AppConfig(
                promptDelete = json.optBoolean("promptDelete", false),
                attachmentFolderPath = json.optString("attachmentFolderPath", null)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Read file content from DocumentFile
     */
    private fun readFileContent(file: DocumentFile): String {
        return context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: ""
    }
}
