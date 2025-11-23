package app.notedrop.android.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for Template domain model.
 */
class TemplateTest {

    @Test
    fun `template extracts variables correctly`() {
        val content = "# {{title}}\n\n{{content}}\n\nCreated: {{date}}"
        val template = Template(
            name = "Test",
            content = content
        )

        assertThat(template.variables).containsExactly("title", "content", "date")
    }

    @Test
    fun `template extracts no variables from plain text`() {
        val content = "Just plain text without variables"
        val template = Template(
            name = "Plain",
            content = content
        )

        assertThat(template.variables).isEmpty()
    }

    @Test
    fun `template extracts unique variables only`() {
        val content = "{{name}} and {{name}} and {{age}}"
        val template = Template(
            name = "Duplicate",
            content = content
        )

        assertThat(template.variables).containsExactly("name", "age")
    }

    @Test
    fun `quick capture template has correct content`() {
        val template = Template.quickCaptureTemplate()

        assertThat(template.name).isEqualTo("Quick Capture")
        assertThat(template.content).isEqualTo("{{content}}")
        assertThat(template.isBuiltIn).isTrue()
        assertThat(template.variables).containsExactly("content")
    }

    @Test
    fun `daily note template has correct structure`() {
        val template = Template.dailyNoteTemplate()

        assertThat(template.name).isEqualTo("Daily Note")
        assertThat(template.isBuiltIn).isTrue()
        assertThat(template.content).contains("{{date}}")
        assertThat(template.content).contains("{{time}}")
        assertThat(template.content).contains("{{content}}")
        assertThat(template.variables).containsExactly("date", "content", "time")
    }

    @Test
    fun `meeting note template has correct structure`() {
        val template = Template.meetingNoteTemplate()

        assertThat(template.name).isEqualTo("Meeting Note")
        assertThat(template.isBuiltIn).isTrue()
        assertThat(template.content).contains("{{title}}")
        assertThat(template.content).contains("{{date}}")
        assertThat(template.content).contains("{{time}}")
        assertThat(template.content).contains("{{content}}")
        assertThat(template.content).contains("## Attendees")
        assertThat(template.content).contains("## Action Items")
    }

    @Test
    fun `built-in templates returns all three templates`() {
        val templates = Template.builtInTemplates()

        assertThat(templates).hasSize(3)
        assertThat(templates.all { it.isBuiltIn }).isTrue()
        assertThat(templates.map { it.name }).containsExactly(
            "Quick Capture",
            "Daily Note",
            "Meeting Note"
        )
    }

    @Test
    fun `template with complex variables extracts correctly`() {
        val content = """
            {{var1}}
            Some text
            {{var2_with_underscore}}
            More text
            {{var3WithCamelCase}}
        """.trimIndent()

        val template = Template(
            name = "Complex",
            content = content
        )

        assertThat(template.variables).containsExactly(
            "var1",
            "var2_with_underscore",
            "var3WithCamelCase"
        )
    }

    @Test
    fun `template ignores malformed variables`() {
        val content = "{{valid}} { invalid } {{another}} {{ space }}"
        val template = Template(
            name = "Mixed",
            content = content
        )

        // Only properly formatted {{word}} variables should be extracted
        assertThat(template.variables).containsExactly("valid", "another")
    }

    @Test
    fun `template creation with defaults`() {
        val template = Template(
            name = "Simple",
            content = "Simple content"
        )

        assertThat(template.id).isNotEmpty()
        assertThat(template.description).isNull()
        assertThat(template.isBuiltIn).isFalse()
        assertThat(template.usageCount).isEqualTo(0)
        assertThat(template.createdAt).isNotNull()
    }

    @Test
    fun `template with description`() {
        val description = "This is a test template"
        val template = Template(
            name = "Test",
            content = "Content",
            description = description
        )

        assertThat(template.description).isEqualTo(description)
    }

    @Test
    fun `custom template is not built-in`() {
        val template = Template(
            name = "Custom",
            content = "Custom content",
            isBuiltIn = false
        )

        assertThat(template.isBuiltIn).isFalse()
    }
}
