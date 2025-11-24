package app.notedrop.android.data.parser

import app.notedrop.android.domain.model.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MarkdownParser.
 * Handles parsing and serialization of markdown files with YAML frontmatter.
 */
@Singleton
class MarkdownParserImpl @Inject constructor() : MarkdownParser {

    companion object {
        private const val FRONTMATTER_DELIMITER = "---"
        private val INLINE_TAG_REGEX = Regex("""#([a-zA-Z0-9_-]+)""")
        private val WIKI_LINK_REGEX = Regex("""\[\[([^\]|]+)(\|([^\]]+))?\]\]""")
        private val MARKDOWN_LINK_REGEX = Regex("""\[([^\]]+)]\(([^)]+)\)""")
        private val HEADING_REGEX = Regex("""^#+\s+(.+)$""", RegexOption.MULTILINE)
    }

    override fun parse(content: String, config: ParserConfig): ParsedMarkdown {
        val frontmatter = if (config.parseFrontmatter) {
            extractFrontmatter(content)
        } else {
            emptyMap()
        }

        val body = extractBody(content)

        val title = when {
            config.extractTitleFromContent && config.titleFromFirstHeading -> {
                extractFirstHeading(body)
            }
            else -> frontmatter["title"]?.toString()
        }

        val tags = mutableListOf<String>()
        if (config.parseFrontmatter) {
            // Tags from frontmatter
            val frontmatterTags = when (val value = frontmatter["tags"]) {
                is List<*> -> value.mapNotNull { it?.toString() }
                is String -> listOf(value)
                else -> emptyList()
            }
            tags.addAll(frontmatterTags)
        }

        if (config.parseInlineTags) {
            // Tags from content
            val inlineTags = extractInlineTags(body)
            tags.addAll(inlineTags.filter { it !in tags })
        }

        val links = if (config.parseLinks) {
            extractLinks(body, config)
        } else {
            emptyList()
        }

        // Extract additional metadata
        val metadata = mutableMapOf<String, String>()
        frontmatter.forEach { (key, value) ->
            if (key !in listOf("tags", "title", "created", "updated")) {
                metadata[key] = value.toString()
            }
        }

        return ParsedMarkdown(
            content = body,
            title = title,
            frontmatter = frontmatter,
            tags = tags.distinct(),
            links = links,
            metadata = metadata
        )
    }

    override fun serialize(note: Note, config: SerializerConfig): String {
        val builder = StringBuilder()

        // Add frontmatter
        if (config.useFrontmatter) {
            builder.append(FRONTMATTER_DELIMITER).append("\n")

            // Title
            if (note.title != null) {
                builder.append("title: ").append(escapeYamlString(note.title)).append("\n")
            }

            // Dates
            val dateFormatter = DateTimeFormatter.ofPattern(config.dateFormat)
                .withZone(ZoneId.systemDefault())
            builder.append("created: ").append(dateFormatter.format(note.createdAt)).append("\n")
            builder.append("updated: ").append(dateFormatter.format(note.updatedAt)).append("\n")

            // Tags
            if (config.tagsInFrontmatter && note.tags.isNotEmpty()) {
                builder.append("tags:\n")
                note.tags.forEach { tag ->
                    builder.append("  - ").append(tag).append("\n")
                }
            }

            // Additional metadata
            note.metadata.forEach { (key, value) ->
                builder.append(key).append(": ").append(escapeYamlString(value)).append("\n")
            }

            // Voice recording path
            if (note.voiceRecordingPath != null) {
                builder.append("voiceRecording: ").append(escapeYamlString(note.voiceRecordingPath)).append("\n")
            }

            // Custom template
            if (config.frontmatterTemplate != null) {
                builder.append(config.frontmatterTemplate).append("\n")
            }

            builder.append(FRONTMATTER_DELIMITER).append("\n\n")
        }

        // Add title as heading
        if (config.includeTitle && config.titleAsHeading && note.title != null) {
            builder.append("# ").append(note.title).append("\n\n")
        }

        // Add content
        builder.append(note.content)

        // Add inline tags
        if (config.includeInlineTags && !config.tagsInFrontmatter && note.tags.isNotEmpty()) {
            builder.append("\n\n")
            builder.append(note.tags.joinToString(" ") { "#$it" })
        }

        // Add voice recording reference
        if (!config.useFrontmatter && note.voiceRecordingPath != null) {
            builder.append("\n\n---\n")
            builder.append("Voice Recording: `").append(note.voiceRecordingPath).append("`\n")
        }

        return builder.toString()
    }

    override fun extractFrontmatter(content: String): Map<String, Any> {
        if (!content.trimStart().startsWith(FRONTMATTER_DELIMITER)) {
            return emptyMap()
        }

        val lines = content.lines()
        val startIndex = lines.indexOfFirst { it.trim() == FRONTMATTER_DELIMITER }
        if (startIndex == -1) return emptyMap()

        val endIndex = lines.drop(startIndex + 1).indexOfFirst { it.trim() == FRONTMATTER_DELIMITER }
        if (endIndex == -1) return emptyMap()

        val yamlLines = lines.subList(startIndex + 1, startIndex + 1 + endIndex)
        return parseYaml(yamlLines)
    }

    override fun extractBody(content: String): String {
        if (!content.trimStart().startsWith(FRONTMATTER_DELIMITER)) {
            return content
        }

        val lines = content.lines()
        val startIndex = lines.indexOfFirst { it.trim() == FRONTMATTER_DELIMITER }
        if (startIndex == -1) return content

        val endIndex = lines.drop(startIndex + 1).indexOfFirst { it.trim() == FRONTMATTER_DELIMITER }
        if (endIndex == -1) return content

        return lines.drop(startIndex + endIndex + 2).joinToString("\n").trim()
    }

    override fun extractInlineTags(content: String): List<String> {
        return INLINE_TAG_REGEX.findAll(content)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    override fun extractLinks(content: String, config: ParserConfig): List<Link> {
        val links = mutableListOf<Link>()

        when (config.linkPattern) {
            LinkPattern.WIKI_ONLY, LinkPattern.WIKI_AND_MARKDOWN -> {
                // Extract wiki-style links
                WIKI_LINK_REGEX.findAll(content).forEach { match ->
                    val raw = match.value
                    val target = match.groupValues[1].trim()
                    val alias = match.groupValues.getOrNull(3)?.trim()
                    val isEmbed = raw.startsWith("![[")
                    links.add(Link.Wiki(raw, target, alias, isEmbed))
                }
            }
            else -> {}
        }

        when (config.linkPattern) {
            LinkPattern.MARKDOWN_ONLY, LinkPattern.WIKI_AND_MARKDOWN -> {
                // Extract markdown links
                MARKDOWN_LINK_REGEX.findAll(content).forEach { match ->
                    val raw = match.value
                    val text = match.groupValues[1]
                    val url = match.groupValues[2]
                    links.add(Link.Markdown(raw, text, url))
                }
            }
            else -> {}
        }

        return links
    }

    /**
     * Simple YAML parser for frontmatter.
     * Supports basic key-value pairs and lists.
     */
    private fun parseYaml(lines: List<String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var currentKey: String? = null
        val currentList = mutableListOf<String>()

        lines.forEach { line ->
            val trimmed = line.trim()

            when {
                // Skip empty lines
                trimmed.isEmpty() -> {}

                // List item
                trimmed.startsWith("- ") -> {
                    val value = trimmed.substring(2).trim()
                    currentList.add(value)
                }

                // Key-value pair
                trimmed.contains(":") -> {
                    // Save previous list if any
                    if (currentKey != null && currentList.isNotEmpty()) {
                        result[currentKey] = currentList.toList()
                        currentList.clear()
                    }

                    val parts = trimmed.split(":", limit = 2)
                    val key = parts[0].trim()
                    val value = parts.getOrNull(1)?.trim() ?: ""

                    currentKey = key
                    if (value.isNotEmpty()) {
                        // Single value
                        result[key] = unescapeYamlString(value)
                    }
                    // Otherwise, it might be followed by a list
                }

                // Continue previous value
                else -> {
                    currentKey?.let { key ->
                        val existingValue = result[key]
                        if (existingValue is String) {
                            result[key] = "$existingValue $trimmed"
                        }
                    }
                }
            }
        }

        // Save final list if any
        if (currentKey != null && currentList.isNotEmpty()) {
            result[currentKey] = currentList.toList()
        }

        return result
    }

    /**
     * Extract first heading from content.
     */
    private fun extractFirstHeading(content: String): String? {
        return HEADING_REGEX.find(content)?.groupValues?.getOrNull(1)?.trim()
    }

    /**
     * Escape YAML string (handle quotes and special characters).
     */
    private fun escapeYamlString(value: String): String {
        return when {
            value.contains(":") || value.contains("#") || value.contains("\"") -> {
                "\"${value.replace("\"", "\\\"")}\""
            }
            else -> value
        }
    }

    /**
     * Unescape YAML string (remove quotes).
     */
    private fun unescapeYamlString(value: String): String {
        return value.trim('"', '\'')
    }
}
