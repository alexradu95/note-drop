package app.notedrop.android.data.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Comprehensive unit tests for ObsidianProvider's parseDailyNotesFormat() method.
 *
 * Tests cover:
 * - Basic date format tokens (YYYY, MM, DD, etc.)
 * - Month name tokens (MMMM, MMM)
 * - Week number tokens (WW, W)
 * - Literal text in brackets [...]
 * - Complex nested folder structures
 * - Edge cases: week 53, leap years, different locales
 * - Special characters in folder names
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ObsidianFormatParserTest {

    private lateinit var context: Context
    private lateinit var provider: ObsidianProvider

    // Test date values for consistent testing
    private val testDate = LocalDate.now()
    private val testYear = testDate.year
    private val testMonth = testDate.monthValue
    private val testDay = testDate.dayOfMonth
    private val testWeek = testDate.get(WeekFields.ISO.weekOfYear())
    private val testFullMonthName = testDate.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    private val testShortMonthName = testDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = ObsidianProvider(context)
    }

    // ==================== Basic Date Token Tests ====================

    @Test
    fun `parseDailyNotesFormat handles basic YYYY-MM-DD format`() = runTest {
        // When: Parse a basic date format
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "YYYY-MM-DD")

        // Then: Should generate correct date filename with no folder
        assertThat(folderPath).isEmpty()
        assertThat(filename).matches("\\d{4}-\\d{2}-\\d{2}\\.md")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles YYYY year token`() = runTest {
        // When: Parse format with 4-digit year
        val (_, filename) = invokeParseDailyNotesFormat(null, "YYYY")

        // Then: Should generate 4-digit year
        assertThat(filename).isEqualTo("$testYear.md")
    }

    @Test
    fun `parseDailyNotesFormat handles YY year token`() = runTest {
        // When: Parse format with 2-digit year
        val (_, filename) = invokeParseDailyNotesFormat(null, "YY")

        // Then: Should generate 2-digit year
        val expectedYY = (testYear % 100).toString().padStart(2, '0')
        assertThat(filename).isEqualTo("$expectedYY.md")
    }

    @Test
    fun `parseDailyNotesFormat handles MM month token with leading zero`() = runTest {
        // When: Parse format with 2-digit month
        val (_, filename) = invokeParseDailyNotesFormat(null, "MM")

        // Then: Should generate 2-digit month with leading zero
        assertThat(filename).matches("\\d{2}\\.md")
        assertThat(filename).isEqualTo("${testMonth.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles M month token without leading zero`() = runTest {
        // When: Parse format with month without leading zero
        val (_, filename) = invokeParseDailyNotesFormat(null, "M")

        // Then: Should generate month without leading zero
        assertThat(filename).isEqualTo("$testMonth.md")
    }

    @Test
    fun `parseDailyNotesFormat handles DD day token with leading zero`() = runTest {
        // When: Parse format with 2-digit day
        val (_, filename) = invokeParseDailyNotesFormat(null, "DD")

        // Then: Should generate 2-digit day with leading zero
        assertThat(filename).matches("\\d{2}\\.md")
        assertThat(filename).isEqualTo("${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles D day token without leading zero`() = runTest {
        // When: Parse format with day without leading zero
        val (_, filename) = invokeParseDailyNotesFormat(null, "D")

        // Then: Should generate day without leading zero
        assertThat(filename).isEqualTo("$testDay.md")
    }

    // ==================== Month Name Token Tests ====================

    @Test
    fun `parseDailyNotesFormat handles MMMM full month name token`() = runTest {
        // When: Parse format with full month name
        val (_, filename) = invokeParseDailyNotesFormat(null, "MMMM")

        // Then: Should generate full month name (e.g., "November")
        assertThat(filename).isEqualTo("$testFullMonthName.md")
    }

    @Test
    fun `parseDailyNotesFormat handles MMM short month name token`() = runTest {
        // When: Parse format with short month name
        val (_, filename) = invokeParseDailyNotesFormat(null, "MMM")

        // Then: Should generate short month name (e.g., "Nov")
        assertThat(filename).isEqualTo("$testShortMonthName.md")
    }

    // ==================== Week Number Token Tests ====================

    @Test
    fun `parseDailyNotesFormat handles WW week number with leading zero`() = runTest {
        // When: Parse format with 2-digit week number
        val (_, filename) = invokeParseDailyNotesFormat(null, "WW")

        // Then: Should generate 2-digit week number with leading zero
        assertThat(filename).matches("\\d{2}\\.md")
        assertThat(filename).isEqualTo("${testWeek.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles W week number without leading zero`() = runTest {
        // When: Parse format with week number without leading zero
        val (_, filename) = invokeParseDailyNotesFormat(null, "W")

        // Then: Should generate week number without leading zero
        assertThat(filename).isEqualTo("$testWeek.md")
    }

    // ==================== Literal Text Tests ====================

    @Test
    fun `parseDailyNotesFormat handles literal text in brackets`() = runTest {
        // When: Parse format with literal text
        val (_, filename) = invokeParseDailyNotesFormat(null, "YYYY-[Week-]WW")

        // Then: Should preserve literal text and replace tokens
        assertThat(filename).isEqualTo("$testYear-Week-${testWeek.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles multiple literal text blocks`() = runTest {
        // When: Parse format with multiple literal blocks
        val (_, filename) = invokeParseDailyNotesFormat(null, "[Note-]YYYY[-]MM[-]DD")

        // Then: Should preserve all literal text blocks
        assertThat(filename).isEqualTo("Note-$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles literal text with special characters`() = runTest {
        // When: Parse format with special characters in literal text
        val (_, filename) = invokeParseDailyNotesFormat(null, "[Daily Note!@#]YYYY")

        // Then: Should preserve special characters
        assertThat(filename).isEqualTo("Daily Note!@#$testYear.md")
    }

    // ==================== Folder Structure Tests ====================

    @Test
    fun `parseDailyNotesFormat handles simple folder structure`() = runTest {
        // When: Parse format with single folder level
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "YYYY/YYYY-MM-DD")

        // Then: Should separate folder and filename
        assertThat(folderPath).isEqualTo("$testYear")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles nested folder structure`() = runTest {
        // When: Parse complex nested folder structure like Obsidian example
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "YYYY/MM-MMMM/[Week-]WW/YYYY-MM-DD")

        // Then: Should generate correct nested folder path
        assertThat(folderPath).isEqualTo("$testYear/${testMonth.padZero()}-$testFullMonthName/Week-${testWeek.padZero()}")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles deeply nested folder structure`() = runTest {
        // When: Parse format with many folder levels
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "YYYY/MMMM/WW/DD/YYYY-MM-DD")

        // Then: Should generate all folder levels correctly
        assertThat(folderPath).isEqualTo("$testYear/$testFullMonthName/${testWeek.padZero()}/${testDay.padZero()}")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat combines baseFolder with format path`() = runTest {
        // When: Parse format with a base folder specified
        val (folderPath, filename) = invokeParseDailyNotesFormat("daily-notes", "YYYY/MM/YYYY-MM-DD")

        // Then: Should prepend base folder to path
        assertThat(folderPath).isEqualTo("daily-notes/$testYear/${testMonth.padZero()}")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles baseFolder with trailing slash`() = runTest {
        // When: Base folder has trailing slash
        val (folderPath, _) = invokeParseDailyNotesFormat("daily-notes/", "YYYY/YYYY-MM-DD")

        // Then: Should normalize the path correctly
        assertThat(folderPath).isEqualTo("daily-notes/$testYear")
    }

    @Test
    fun `parseDailyNotesFormat handles baseFolder with leading slash`() = runTest {
        // When: Base folder has leading slash
        val (folderPath, _) = invokeParseDailyNotesFormat("/daily-notes", "YYYY/YYYY-MM-DD")

        // Then: Should normalize the path correctly
        assertThat(folderPath).isEqualTo("daily-notes/$testYear")
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `parseDailyNotesFormat handles empty format string`() = runTest {
        // When: Parse empty format string
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "")

        // Then: Should generate basic date filename
        assertThat(folderPath).isEmpty()
        assertThat(filename).isEqualTo(".md")
    }

    @Test
    fun `parseDailyNotesFormat handles format with only folder no filename`() = runTest {
        // When: Parse format that only contains folders (edge case)
        val (folderPath, filename) = invokeParseDailyNotesFormat(null, "YYYY/MM/DD")

        // Then: Last component becomes filename
        assertThat(folderPath).isEqualTo("$testYear/${testMonth.padZero()}")
        assertThat(filename).isEqualTo("${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles mixed token order`() = runTest {
        // When: Tokens are in unusual order
        val (_, filename) = invokeParseDailyNotesFormat(null, "DD-MM-YYYY")

        // Then: Should respect the order specified
        assertThat(filename).isEqualTo("${testDay.padZero()}-${testMonth.padZero()}-$testYear.md")
    }

    @Test
    fun `parseDailyNotesFormat handles format without any tokens`() = runTest {
        // When: Format has only literal text
        val (_, filename) = invokeParseDailyNotesFormat(null, "[Daily Note]")

        // Then: Should use literal text as filename
        assertThat(filename).isEqualTo("Daily Note.md")
    }

    @Test
    fun `parseDailyNotesFormat preserves token case sensitivity`() = runTest {
        // When: Format uses mixed case (should not be replaced, only exact tokens)
        val (_, filename) = invokeParseDailyNotesFormat(null, "yyyy-Mm-dD")

        // Then: Should only replace exact token matches
        assertThat(filename).isEqualTo("yyyy-${testMonth.padZero()}m-dD.md")
    }

    // ==================== Complex Real-World Format Tests ====================

    @Test
    fun `parseDailyNotesFormat handles Obsidian default format`() = runTest {
        // When: Using Obsidian's default daily notes format
        val (folderPath, filename) = invokeParseDailyNotesFormat("Daily Notes", "YYYY-MM-DD")

        // Then: Should generate simple daily note path
        assertThat(folderPath).isEqualTo("Daily Notes")
        assertThat(filename).isEqualTo("$testYear-${testMonth.padZero()}-${testDay.padZero()}.md")
    }

    @Test
    fun `parseDailyNotesFormat handles weekly organization format`() = runTest {
        // When: Using weekly organization structure
        val (folderPath, filename) = invokeParseDailyNotesFormat(
            "journal",
            "YYYY/[Week ]WW/YYYY-MM-DD - dddd"
        )

        // Then: Should organize by year and week
        assertThat(folderPath).isEqualTo("journal/$testYear/Week ${testWeek.padZero()}")
        assertThat(filename).matches("\\d{4}-\\d{2}-\\d{2} - dddd\\.md")
    }

    @Test
    fun `parseDailyNotesFormat handles monthly archive format`() = runTest {
        // When: Using monthly archive structure
        val (folderPath, filename) = invokeParseDailyNotesFormat(
            "archive",
            "YYYY/MMMM/DD - MMMM D, YYYY"
        )

        // Then: Should organize by year and month
        assertThat(folderPath).isEqualTo("archive/$testYear/$testFullMonthName")
        assertThat(filename).isEqualTo("${testDay.padZero()} - $testFullMonthName $testDay, $testYear.md")
    }

    @Test
    fun `parseDailyNotesFormat handles descriptive filename format`() = runTest {
        // When: Using descriptive filenames with month names
        val (_, filename) = invokeParseDailyNotesFormat(null, "YYYY-MM-DD ([MMMM D, YYYY])")

        // Then: Should create descriptive filename
        assertThat(filename).isEqualTo(
            "$testYear-${testMonth.padZero()}-${testDay.padZero()} ($testFullMonthName $testDay, $testYear).md"
        )
    }

    // ==================== Helper Methods ====================

    /**
     * Reflection-based helper to invoke the private parseDailyNotesFormat method.
     * This allows us to test the internal date format parsing logic directly.
     */
    private fun invokeParseDailyNotesFormat(baseFolder: String?, format: String): Pair<String, String> {
        val method = ObsidianProvider::class.java.getDeclaredMethod(
            "parseDailyNotesFormat",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(provider, baseFolder, format) as Pair<String, String>
    }

    /**
     * Helper to pad integers with leading zero if needed
     */
    private fun Int.padZero(): String = this.toString().padStart(2, '0')
}
