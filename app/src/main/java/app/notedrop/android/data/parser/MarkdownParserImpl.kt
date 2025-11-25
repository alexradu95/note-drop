package app.notedrop.android.data.parser

import app.notedrop.android.domain.model.Note
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser as JetBrainsMarkdownParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MarkdownParser using JetBrains Markdown library.
 * Handles parsing and serialization of markdown files with YAML frontmatter.
 */
@Singleton
class MarkdownParserImpl @Inject constructor() : MarkdownParser {

    companion object {
        private const val FRONTMATTER_DELIMITER = "---"
        private val INLINE_TAG_REGEX = Regex("""#([a-zA-Z0-9_-]+)""")
        private val WIKI_LINK_REGEX = Regex("""(!?)\[\[([^\]|]+)(\|([^\]]+))?\]\]""")
    }

    private val markdownFlavour = CommonMarkFlavourDescriptor()

    override fun parse(content: String, config: ParserConfig): ParsedMarkdown {
        val frontmatter = if (config.parseFrontmatter) {
            extractFrontmatter(content)
        } else {
            emptyMap()
        }

        val body = extractBody(content)

        // Extract title and remove it from body if configured
        val (title, contentWithoutTitle) = when {
            config.extractTitleFromContent && config.titleFromFirstHeading -> {
                val extractedTitle = extractFirstHeading(body)
                if (extractedTitle != null) {
                    // Remove the first heading from body
                    val bodyWithoutTitle = body.replaceFirst(
                        Regex("""^#+\s+${Regex.escape(extractedTitle)}\s*\n+""", RegexOption.MULTILINE),
                        ""
                    )
                    extractedTitle to bodyWithoutTitle
                } else {
                    (frontmatter["title"]?.toString() to body)
                }
            }
            else -> (frontmatter["title"]?.toString() to body)
        }

        val finalContent = contentWithoutTitle

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
            val inlineTags = extractInlineTags(finalContent)
            tags.addAll(inlineTags.filter { it !in tags })
        }

        val links = if (config.parseLinks) {
            extractLinks(finalContent, config)
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
            content = finalContent,
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

        // Join body lines and trim only leading whitespace
        return lines.drop(startIndex + endIndex + 2).joinToString("\n").trimStart()
    }

    override fun extractInlineTags(content: String): List<String> {
        return INLINE_TAG_REGEX.findAll(content)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    override fun extractLinks(content: String, config: ParserConfig): List<Link> {
        val links = mutableListOf<Link>()

        // Extract wiki-style links first (custom syntax not supported by standard markdown)
        when (config.linkPattern) {
            LinkPattern.WIKI_ONLY, LinkPattern.WIKI_AND_MARKDOWN -> {
                WIKI_LINK_REGEX.findAll(content).forEach { match ->
                    val raw = match.value
                    val isEmbed = match.groupValues[1] == "!"
                    val target = match.groupValues[2].trim()
                    val aliasRaw = match.groupValues.getOrNull(4)?.trim()
                    val alias = if (aliasRaw.isNullOrEmpty()) null else aliasRaw
                    links.add(Link.Wiki(raw, target, alias, isEmbed))
                }
            }
            else -> {}
        }

        // Extract markdown links using JetBrains parser
        when (config.linkPattern) {
            LinkPattern.MARKDOWN_ONLY, LinkPattern.WIKI_AND_MARKDOWN -> {
                try {
                    val parsedTree = JetBrainsMarkdownParser(markdownFlavour).buildMarkdownTreeFromString(content)
                    extractMarkdownLinksFromNode(parsedTree, content, links)
                } catch (e: Exception) {
                    // Fallback to regex if parsing fails
                    extractMarkdownLinksWithRegex(content, links)
                }
            }
            else -> {}
        }

        return links
    }

    /**
     * Extract markdown links from AST node recursively.
     */
    private fun extractMarkdownLinksFromNode(node: ASTNode, content: String, links: MutableList<Link>) {
        if (node.type == MarkdownElementTypes.INLINE_LINK) {
            val linkText = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
            val linkDestination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }

            if (linkText != null && linkDestination != null) {
                val text = linkText.getTextInNode(content).toString().trim('[', ']')
                val url = linkDestination.getTextInNode(content).toString()
                val raw = node.getTextInNode(content).toString()
                links.add(Link.Markdown(raw, text, url))
            }
        }

        node.children.forEach { child ->
            extractMarkdownLinksFromNode(child, content, links)
        }
    }

    /**
     * Fallback regex-based markdown link extraction.
     */
    private fun extractMarkdownLinksWithRegex(content: String, links: MutableList<Link>) {
        val regex = Regex("""\[([^\]]+)]\(([^)]+)\)""")
        regex.findAll(content).forEach { match ->
            val raw = match.value
            val text = match.groupValues[1]
            val url = match.groupValues[2]
            links.add(Link.Markdown(raw, text, url))
        }
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
     * Extract first heading from content using JetBrains Markdown parser.
     */
    private fun extractFirstHeading(content: String): String? {
        return try {
            val parsedTree = JetBrainsMarkdownParser(markdownFlavour).buildMarkdownTreeFromString(content)
            findFirstHeading(parsedTree, content)
        } catch (e: Exception) {
            // Fallback to regex if parsing fails
            Regex("""^#+\s+(.+)$""", RegexOption.MULTILINE)
                .find(content)?.groupValues?.getOrNull(1)?.trim()
        }
    }

    /**
     * Recursively find first heading in AST.
     */
    private fun findFirstHeading(node: ASTNode, content: String): String? {
        if (node.type == MarkdownElementTypes.ATX_1 ||
            node.type == MarkdownElementTypes.ATX_2 ||
            node.type == MarkdownElementTypes.ATX_3 ||
            node.type == MarkdownElementTypes.ATX_4 ||
            node.type == MarkdownElementTypes.ATX_5 ||
            node.type == MarkdownElementTypes.ATX_6) {

            // Extract text content from heading, excluding the # symbols
            val textContent = node.children
                .filter { it.type != MarkdownTokenTypes.ATX_HEADER }
                .joinToString("") { it.getTextInNode(content).toString() }
                .trim()

            return textContent
        }

        for (child in node.children) {
            val heading = findFirstHeading(child, content)
            if (heading != null) return heading
        }

        return null
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
