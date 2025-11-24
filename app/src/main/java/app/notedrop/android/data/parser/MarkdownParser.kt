package app.notedrop.android.data.parser

import app.notedrop.android.domain.model.Note

/**
 * Generic markdown parser for file-based providers.
 * Handles parsing and serialization of markdown files with metadata.
 *
 * This is provider-agnostic - different providers can use different
 * configurations to customize the parsing behavior.
 */
interface MarkdownParser {

    /**
     * Parse markdown content into a Note object.
     * @param content Raw markdown content
     * @param config Parser configuration
     * @return Parsed markdown structure
     */
    fun parse(content: String, config: ParserConfig = ParserConfig()): ParsedMarkdown

    /**
     * Serialize a Note object into markdown content.
     * @param note The note to serialize
     * @param config Serializer configuration
     * @return Markdown content as string
     */
    fun serialize(note: Note, config: SerializerConfig = SerializerConfig()): String

    /**
     * Extract frontmatter from markdown content.
     * @param content Markdown content with frontmatter
     * @return Map of frontmatter key-value pairs
     */
    fun extractFrontmatter(content: String): Map<String, Any>

    /**
     * Extract body content (without frontmatter).
     * @param content Markdown content
     * @return Body content without frontmatter
     */
    fun extractBody(content: String): String

    /**
     * Extract inline tags from content (e.g., #tag).
     * @param content Markdown content
     * @return List of tags
     */
    fun extractInlineTags(content: String): List<String>

    /**
     * Extract links from content.
     * Supports both markdown links [text](url) and wiki-style [[note]].
     * @param content Markdown content
     * @param config Parser configuration
     * @return List of links
     */
    fun extractLinks(content: String, config: ParserConfig = ParserConfig()): List<Link>
}

/**
 * Result of parsing markdown content.
 */
data class ParsedMarkdown(
    val content: String,
    val title: String?,
    val frontmatter: Map<String, Any>,
    val tags: List<String>,
    val links: List<Link>,
    val metadata: Map<String, String>
) {
    /**
     * Get frontmatter value as string.
     */
    fun getFrontmatterString(key: String): String? {
        return frontmatter[key]?.toString()
    }

    /**
     * Get frontmatter value as list of strings.
     */
    @Suppress("UNCHECKED_CAST")
    fun getFrontmatterList(key: String): List<String>? {
        return when (val value = frontmatter[key]) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> listOf(value)
            else -> null
        }
    }
}

/**
 * Configuration for markdown parsing.
 */
data class ParserConfig(
    val parseFrontmatter: Boolean = true,
    val parseInlineTags: Boolean = true,
    val parseLinks: Boolean = true,
    val linkPattern: LinkPattern = LinkPattern.WIKI_AND_MARKDOWN,
    val extractTitleFromContent: Boolean = true,
    val titleFromFirstHeading: Boolean = true
)

/**
 * Configuration for markdown serialization.
 */
data class SerializerConfig(
    val useFrontmatter: Boolean = true,
    val frontmatterTemplate: String? = null,
    val includeTitle: Boolean = true,
    val titleAsHeading: Boolean = true,
    val includeInlineTags: Boolean = false,
    val tagsInFrontmatter: Boolean = true,
    val dateFormat: String = "yyyy-MM-dd'T'HH:mm:ss'Z'"
)

/**
 * Supported link patterns.
 */
enum class LinkPattern {
    /**
     * Only markdown links: [text](url)
     */
    MARKDOWN_ONLY,

    /**
     * Only wiki-style links: [[note]] or [[note|alias]]
     */
    WIKI_ONLY,

    /**
     * Both markdown and wiki-style links
     */
    WIKI_AND_MARKDOWN
}

/**
 * Represents a link in markdown content.
 */
sealed class Link {
    abstract val raw: String

    /**
     * Standard markdown link: [text](url)
     */
    data class Markdown(
        override val raw: String,
        val text: String,
        val url: String
    ) : Link()

    /**
     * Wiki-style link: [[target]] or [[target|alias]]
     */
    data class Wiki(
        override val raw: String,
        val target: String,
        val alias: String?,
        val isEmbed: Boolean = false
    ) : Link() {
        val displayText: String
            get() = alias ?: target
    }
}
