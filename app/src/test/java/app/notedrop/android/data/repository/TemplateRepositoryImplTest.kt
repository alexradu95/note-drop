package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.Template
import app.notedrop.android.util.MainDispatcherRule
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for TemplateRepositoryImpl with mocked DAO.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TemplateRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var templateDao: TemplateDao
    private lateinit var repository: TemplateRepositoryImpl

    @Before
    fun setup() {
        templateDao = mockk()
        repository = TemplateRepositoryImpl(templateDao)
    }

    @Test
    fun `getAllTemplates returns flow of templates`() = runTest {
        val templates = TestFixtures.createTemplates(3)
        every { templateDao.getAllTemplates() } returns flowOf(templates.map { it.toEntity() })

        val result = repository.getAllTemplates().first()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.name }).containsExactly(
            "Test Template 1",
            "Test Template 2",
            "Test Template 3"
        )
    }

    @Test
    fun `getTemplateById returns template when exists`() = runTest {
        val template = TestFixtures.createTemplate()
        coEvery { templateDao.getTemplateById(template.id) } returns template.toEntity()

        val result = repository.getTemplateById(template.id)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(template.id)
        assertThat(result?.name).isEqualTo(template.name)
    }

    @Test
    fun `getTemplateById returns null when not found`() = runTest {
        coEvery { templateDao.getTemplateById(any()) } returns null

        val result = repository.getTemplateById("non-existent")

        assertThat(result).isNull()
    }

    @Test
    fun `getBuiltInTemplates returns only built-in templates`() = runTest {
        val builtInTemplates = Template.builtInTemplates()
        every { templateDao.getBuiltInTemplates() } returns flowOf(builtInTemplates.map { it.toEntity() })

        val result = repository.getBuiltInTemplates().first()

        assertThat(result).isNotEmpty()
        assertThat(result.all { it.isBuiltIn }).isTrue()
    }

    @Test
    fun `getUserTemplates returns only user templates`() = runTest {
        val userTemplates = TestFixtures.createTemplates(2).map { it.copy(isBuiltIn = false) }
        every { templateDao.getUserTemplates() } returns flowOf(userTemplates.map { it.toEntity() })

        val result = repository.getUserTemplates().first()

        assertThat(result).hasSize(2)
        assertThat(result.all { !it.isBuiltIn }).isTrue()
    }

    @Test
    fun `searchTemplates filters by query`() = runTest {
        val templates = listOf(
            TestFixtures.createTemplate(name = "Meeting Notes"),
            TestFixtures.createTemplate(name = "Daily Journal")
        )
        every { templateDao.searchTemplates("meeting") } returns flowOf(
            templates.filter { it.name.contains("Meeting", ignoreCase = true) }.map { it.toEntity() }
        )

        val result = repository.searchTemplates("meeting").first()

        assertThat(result).hasSize(1)
        assertThat(result.first().name).contains("Meeting")
    }

    @Test
    fun `createTemplate inserts template successfully`() = runTest {
        val template = TestFixtures.createTemplate()
        coEvery { templateDao.insertTemplate(any()) } just Runs

        val result = repository.createTemplate(template)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(template)
        coVerify { templateDao.insertTemplate(template.toEntity()) }
    }

    @Test
    fun `createTemplate handles errors`() = runTest {
        val template = TestFixtures.createTemplate()
        val exception = Exception("Database error")
        coEvery { templateDao.insertTemplate(any()) } throws exception

        val result = repository.createTemplate(template)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `updateTemplate updates existing template`() = runTest {
        val template = TestFixtures.createTemplate()
        coEvery { templateDao.updateTemplate(any()) } just Runs

        val result = repository.updateTemplate(template)

        assertThat(result.isSuccess).isTrue()
        coVerify { templateDao.updateTemplate(template.toEntity()) }
    }

    @Test
    fun `deleteTemplate removes template by ID`() = runTest {
        val templateId = "template-1"
        coEvery { templateDao.deleteTemplateById(templateId) } just Runs

        val result = repository.deleteTemplate(templateId)

        assertThat(result.isSuccess).isTrue()
        coVerify { templateDao.deleteTemplateById(templateId) }
    }

    @Test
    fun `incrementUsageCount increments template usage`() = runTest {
        val templateId = "template-1"
        coEvery { templateDao.incrementUsageCount(templateId) } just Runs

        val result = repository.incrementUsageCount(templateId)

        assertThat(result.isSuccess).isTrue()
        coVerify { templateDao.incrementUsageCount(templateId) }
    }

    @Test
    fun `initializeBuiltInTemplates inserts templates when database is empty`() = runTest {
        coEvery { templateDao.getTemplateCount() } returns 0
        coEvery { templateDao.insertTemplates(any()) } just Runs

        val result = repository.initializeBuiltInTemplates()

        assertThat(result.isSuccess).isTrue()
        coVerify { templateDao.insertTemplates(any()) }
    }

    @Test
    fun `initializeBuiltInTemplates skips insertion when templates exist`() = runTest {
        coEvery { templateDao.getTemplateCount() } returns 5

        val result = repository.initializeBuiltInTemplates()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { templateDao.insertTemplates(any()) }
    }
}
