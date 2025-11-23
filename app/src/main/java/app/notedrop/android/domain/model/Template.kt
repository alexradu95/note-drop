package app.notedrop.android.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model representing a note template.
 *
 * @property id Unique identifier for the template
 * @property name User-friendly name of the template
 * @property content Template content with variables (e.g., {{date}}, {{time}}, {{title}})
 * @property description Optional description
 * @property variables List of variable names used in the template
 * @property isBuiltIn Whether this is a built-in template (cannot be deleted)
 * @property createdAt Timestamp when the template was created
 * @property usageCount Number of times this template has been used
 */
data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val description: String? = null,
    val variables: List<String> = extractVariables(content),
    val isBuiltIn: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val usageCount: Int = 0
) {
    companion object {
        /**
         * Extract variable names from template content.
         * Variables are in the format: {{variableName}}
         */
        private fun extractVariables(content: String): List<String> {
            val regex = """\{\{(\w+)\}\}""".toRegex()
            return regex.findAll(content)
                .map { it.groupValues[1] }
                .distinct()
                .toList()
        }

        /**
         * Built-in template for quick capture.
         */
        fun quickCaptureTemplate() = Template(
            id = "builtin_quick_capture",
            name = "Quick Capture",
            content = "{{content}}",
            description = "Simple note with just the content",
            isBuiltIn = true
        )

        /**
         * Built-in template for daily notes.
         */
        fun dailyNoteTemplate() = Template(
            id = "builtin_daily_note",
            name = "Daily Note",
            content = """
                # {{date}}

                ## Notes
                {{content}}

                ---
                Created at {{time}}
            """.trimIndent(),
            description = "Daily note with date header",
            isBuiltIn = true
        )

        /**
         * Built-in template for meeting notes.
         */
        fun meetingNoteTemplate() = Template(
            id = "builtin_meeting",
            name = "Meeting Note",
            content = """
                # Meeting: {{title}}
                Date: {{date}}
                Time: {{time}}

                ## Attendees
                -

                ## Notes
                {{content}}

                ## Action Items
                - [ ]
            """.trimIndent(),
            description = "Meeting notes with action items",
            isBuiltIn = true
        )

        /**
         * Get all built-in templates.
         */
        fun builtInTemplates() = listOf(
            quickCaptureTemplate(),
            dailyNoteTemplate(),
            meetingNoteTemplate()
        )
    }
}
