package app.notedrop.android.data.provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.notedrop.android.domain.model.ProviderConfig
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for ObsidianProvider using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ObsidianProviderTest {

    private lateinit var context: Context
    private lateinit var provider: ObsidianProvider
    private lateinit var testVaultDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = ObsidianProvider(context)

        // Create a temporary test vault directory
        testVaultDir = File(context.cacheDir, "test-vault")
        testVaultDir.mkdirs()
    }

    @After
    fun tearDown() {
        // Clean up test vault directory
        testVaultDir.deleteRecursively()
    }

    @Test
    fun `saveNote creates markdown file`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Test Note",
            content = "Test content"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        // Verify file was created
        val files = testVaultDir.listFiles()
        assertThat(files).isNotNull()
        assertThat(files).hasLength(1)
        assertThat(files?.first()?.name).endsWith(".md")
    }

    @Test
    fun `saveNote with front matter includes metadata`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = true,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Test Note",
            content = "Test content",
            tags = listOf("test", "important")
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        // Read the file and verify front matter
        val file = testVaultDir.listFiles()?.first()
        val content = file?.readText()

        assertThat(content).contains("---")
        assertThat(content).contains("created:")
        assertThat(content).contains("updated:")
        assertThat(content).contains("tags:")
        assertThat(content).contains("  - test")
        assertThat(content).contains("  - important")
    }

    @Test
    fun `saveNote without front matter includes inline tags`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Test Note",
            content = "Test content",
            tags = listOf("test", "important")
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        val content = file?.readText()

        assertThat(content).contains("#test")
        assertThat(content).contains("#important")
    }

    @Test
    fun `saveNote with title includes heading`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "My Important Note",
            content = "This is the content"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        val content = file?.readText()

        assertThat(content).contains("# My Important Note")
        assertThat(content).contains("This is the content")
    }

    @Test
    fun `saveNote with daily notes path creates subdirectory`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = "daily",
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Daily Note",
            content = "Today's content"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val dailyDir = File(testVaultDir, "daily")
        assertThat(dailyDir.exists()).isTrue()
        assertThat(dailyDir.isDirectory).isTrue()
        assertThat(dailyDir.listFiles()).isNotEmpty()
    }

    @Test
    fun `saveNote with voice recording adds reference`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Voice Note",
            content = "Transcribed content",
            voiceRecordingPath = "/storage/recordings/voice_123.m4a"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        val content = file?.readText()

        assertThat(content).contains("Voice Recording:")
        assertThat(content).contains("voice_123.m4a")
    }

    @Test
    fun `saveNote sanitizes title for filename`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = "Test Note: With Special/Characters!",
            content = "Content"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        // Filename should not contain special characters
        assertThat(file?.name).doesNotContain(":")
        assertThat(file?.name).doesNotContain("/")
        assertThat(file?.name).doesNotContain("!")
    }

    @Test
    fun `saveNote without title uses timestamp filename`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )
        val note = TestFixtures.createNote(
            title = null,
            content = "Content without title"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        // Filename should be a timestamp format
        assertThat(file?.name).matches(Regex("\\d{4}-\\d{2}-\\d{2}-\\d{6}\\.md"))
    }

    @Test
    fun `saveNote with custom front matter template adds custom fields`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = true,
                frontMatterTemplate = "author: NoteDrop\ntype: quick-note"
            )
        )
        val note = TestFixtures.createNote(
            title = "Test",
            content = "Content"
        )

        val result = provider.saveNote(note, vault)

        assertThat(result.isSuccess).isTrue()

        val file = testVaultDir.listFiles()?.first()
        val content = file?.readText()

        assertThat(content).contains("author: NoteDrop")
        assertThat(content).contains("type: quick-note")
    }

    @Test
    fun `saveNote handles invalid config`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.LOCAL,
            providerConfig = ProviderConfig.LocalConfig("/local/path")
        )
        val note = TestFixtures.createNote()

        val result = provider.saveNote(note, vault)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `isAvailable returns true for valid vault directory`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = testVaultDir.absolutePath,
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )

        val available = provider.isAvailable(vault)

        assertThat(available).isTrue()
    }

    @Test
    fun `isAvailable returns false for non-existent directory`() = runTest {
        val vault = TestFixtures.createVault(
            providerType = ProviderType.OBSIDIAN,
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/non/existent/path",
                dailyNotesPath = null,
                templatePath = null,
                useFrontMatter = false,
                frontMatterTemplate = null
            )
        )

        val available = provider.isAvailable(vault)

        assertThat(available).isFalse()
    }

    @Test
    fun `getCapabilities returns correct feature support`() {
        val capabilities = provider.getCapabilities()

        assertThat(capabilities.supportsVoiceRecordings).isTrue()
        assertThat(capabilities.supportsImages).isTrue()
        assertThat(capabilities.supportsTags).isTrue()
        assertThat(capabilities.supportsMetadata).isTrue()
        assertThat(capabilities.supportsEncryption).isFalse()
        assertThat(capabilities.requiresInternet).isFalse()
    }

    @Test
    fun `getDailyNotePath generates correct path`() {
        val config = ProviderConfig.ObsidianConfig(
            vaultPath = testVaultDir.absolutePath,
            dailyNotesPath = "daily-notes",
            templatePath = null,
            useFrontMatter = false,
            frontMatterTemplate = null
        )

        val path = provider.getDailyNotePath(config)

        assertThat(path).startsWith("daily-notes/")
        assertThat(path).endsWith(".md")
        assertThat(path).matches(Regex("daily-notes/\\d{4}-\\d{2}-\\d{2}\\.md"))
    }
}
