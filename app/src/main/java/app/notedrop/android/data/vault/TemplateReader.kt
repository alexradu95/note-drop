package app.notedrop.android.data.vault

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Template
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads templates directly from the vault's template folder.
 *
 * Templates are stored as markdown files in the vault's configured template folder.
 * Each template file can have frontmatter metadata:
 *
 * ```markdown
 * ---
 * name: Meeting Template
 * description: Template for meeting notes
 * ---
 *
 * # Meeting: {{title}}
 * ...
 * ```
 *
 * If no frontmatter exists, the filename (without .md) becomes the template name.
 *
 * Falls back to built-in templates if vault templates don't exist or can't be read.
 */
@Singleton
class TemplateReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Get all templates from vault folder.
     *
     * @param vaultUri The content URI of the vault root folder
     * @param templatePath The relative path to template folder (from vault config)
     * @return Result containing list of templates (including built-ins as fallback)
     */
    fun getAllTemplates(
        vaultUri: Uri,
        templatePath: String?
    ): Result<List<Template>, AppError> {
        return try {
            val templates = mutableListOf<Template>()

            // Try to read vault templates
            val vaultTemplates = readVaultTemplates(vaultUri, templatePath)
            if (vaultTemplates.isNotEmpty()) {
                templates.addAll(vaultTemplates)
                android.util.Log.d(TAG, "Loaded ${vaultTemplates.size} templates from vault")
            } else {
                android.util.Log.d(TAG, "No vault templates found, using built-ins only")
            }

            // Always include built-in templates
            templates.addAll(Template.builtInTemplates())

            Ok(templates)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to read templates", e)
            // Even on error, return built-in templates as fallback
            Ok(Template.builtInTemplates())
        }
    }

    /**
     * Get a specific template by name.
     *
     * @param vaultUri The content URI of the vault root folder
     * @param templatePath The relative path to template folder
     * @param name The template name to find
     * @return Result containing the template or error if not found
     */
    fun getTemplateByName(
        vaultUri: Uri,
        templatePath: String?,
        name: String
    ): Result<Template, AppError> {
        return try {
            getAllTemplates(vaultUri, templatePath).let { result ->
                when (result) {
                    is Ok -> {
                        val template = result.value.find { it.name.equals(name, ignoreCase = true) }
                        if (template != null) {
                            Ok(template)
                        } else {
                            Err(AppError.Database.NotFound("Template", name))
                        }
                    }
                    is Err -> result
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to get template by name: $name", e)
            Err(AppError.Validation.FieldError("name", "Template not found: $name"))
        }
    }

    /**
     * Read templates from vault's template folder.
     */
    private fun readVaultTemplates(vaultUri: Uri, templatePath: String?): List<Template> {
        try {
            val vaultRoot = DocumentFile.fromTreeUri(context, vaultUri) ?: return emptyList()

            // Find template folder
            val templateFolder = if (templatePath.isNullOrBlank()) {
                // Try common template folder names
                vaultRoot.findFile("templates")
                    ?: vaultRoot.findFile("Templates")
                    ?: vaultRoot.findFile("_templates")
                    ?: return emptyList()
            } else {
                // Navigate to configured template path
                findFolderByPath(vaultRoot, templatePath) ?: return emptyList()
            }

            if (!templateFolder.exists() || !templateFolder.isDirectory) {
                android.util.Log.d(TAG, "Template folder not found or not a directory")
                return emptyList()
            }

            // Read all .md files in template folder
            val templateFiles = templateFolder.listFiles().filter {
                it.isFile && it.name?.endsWith(".md", ignoreCase = true) == true
            }

            android.util.Log.d(TAG, "Found ${templateFiles.size} template files in vault")

            return templateFiles.mapNotNull { file ->
                parseTemplateFile(file)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error reading vault templates", e)
            return emptyList()
        }
    }

    /**
     * Parse a template markdown file.
     */
    private fun parseTemplateFile(file: DocumentFile): Template? {
        return try {
            val content = readFileContent(file)
            val fileName = file.name?.removeSuffix(".md") ?: "Unknown Template"

            // Try to parse frontmatter
            val frontmatterMatch = FRONTMATTER_REGEX.find(content)

            val (name, description, templateContent) = if (frontmatterMatch != null) {
                val frontmatter = frontmatterMatch.groupValues[1]
                val body = content.substring(frontmatterMatch.range.last + 1).trim()

                val parsedName = extractFrontmatterField(frontmatter, "name") ?: fileName
                val parsedDesc = extractFrontmatterField(frontmatter, "description")

                Triple(parsedName, parsedDesc, body)
            } else {
                // No frontmatter, use filename and full content
                Triple(fileName, null, content)
            }

            Template(
                id = "vault_${UUID.randomUUID()}",
                name = name,
                content = templateContent,
                description = description,
                isBuiltIn = false,
                usageCount = 0
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to parse template file: ${file.name}", e)
            null
        }
    }

    /**
     * Extract a field value from frontmatter.
     */
    private fun extractFrontmatterField(frontmatter: String, fieldName: String): String? {
        val regex = """^$fieldName:\s*(.+)$""".toRegex(RegexOption.MULTILINE)
        return regex.find(frontmatter)?.groupValues?.get(1)?.trim()
    }

    /**
     * Find a folder by relative path.
     */
    private fun findFolderByPath(root: DocumentFile, path: String): DocumentFile? {
        var current = root
        val parts = path.split("/").filter { it.isNotBlank() }

        for (part in parts) {
            current = current.findFile(part) ?: return null
            if (!current.isDirectory) return null
        }

        return current
    }

    /**
     * Read file content from DocumentFile.
     */
    private fun readFileContent(file: DocumentFile): String {
        return context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: ""
    }

    companion object {
        private const val TAG = "TemplateReader"

        // Regex for YAML frontmatter
        private val FRONTMATTER_REGEX = """^---\s*\n(.*?)\n---\s*\n""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
}
