package app.notedrop.android.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.notedrop.android.data.local.NoteDropDatabase
import app.notedrop.android.data.local.entity.TemplateEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class TemplateDaoTest {

    private lateinit var database: NoteDropDatabase
    private lateinit var templateDao: TemplateDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoteDropDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        templateDao = database.templateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTemplate() = runTest {
        val template = createTestTemplate(id = "1", name = "Test Template")
        templateDao.insertTemplate(template)

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo("Test Template")
    }

    @Test
    fun getAllTemplates_ordersBuiltInFirst() = runTest {
        val template1 = createTestTemplate(id = "1", name = "User", isBuiltIn = false, usageCount = 10)
        val template2 = createTestTemplate(id = "2", name = "Built-in", isBuiltIn = true, usageCount = 5)
        val template3 = createTestTemplate(id = "3", name = "User2", isBuiltIn = false, usageCount = 20)

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)
        templateDao.insertTemplate(template3)

        val templates = templateDao.getAllTemplates().first()
        assertThat(templates[0].isBuiltIn).isTrue() // Built-in first
        assertThat(templates[1].usageCount).isEqualTo(20) // Then by usage count
    }

    @Test
    fun getBuiltInTemplates_filtersCorrectly() = runTest {
        val template1 = createTestTemplate(id = "1", isBuiltIn = true)
        val template2 = createTestTemplate(id = "2", isBuiltIn = false)
        val template3 = createTestTemplate(id = "3", isBuiltIn = true)

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)
        templateDao.insertTemplate(template3)

        val builtIn = templateDao.getBuiltInTemplates().first()
        assertThat(builtIn).hasSize(2)
    }

    @Test
    fun getUserTemplates_filtersCorrectly() = runTest {
        val template1 = createTestTemplate(id = "1", isBuiltIn = true)
        val template2 = createTestTemplate(id = "2", isBuiltIn = false)
        val template3 = createTestTemplate(id = "3", isBuiltIn = false)

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)
        templateDao.insertTemplate(template3)

        val userTemplates = templateDao.getUserTemplates().first()
        assertThat(userTemplates).hasSize(2)
    }

    @Test
    fun searchTemplates_findsByName() = runTest {
        val template1 = createTestTemplate(id = "1", name = "Meeting Notes")
        val template2 = createTestTemplate(id = "2", name = "Daily Journal")
        val template3 = createTestTemplate(id = "3", name = "Meeting Summary")

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)
        templateDao.insertTemplate(template3)

        val results = templateDao.searchTemplates("Meeting").first()
        assertThat(results).hasSize(2)
    }

    @Test
    fun searchTemplates_findsByContent() = runTest {
        val template1 = createTestTemplate(id = "1", content = "Contains keyword")
        val template2 = createTestTemplate(id = "2", content = "Does not")
        val template3 = createTestTemplate(id = "3", content = "Also has keyword")

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)
        templateDao.insertTemplate(template3)

        val results = templateDao.searchTemplates("keyword").first()
        assertThat(results).hasSize(2)
    }

    @Test
    fun updateTemplate_modifiesExisting() = runTest {
        val template = createTestTemplate(id = "1", content = "Original")
        templateDao.insertTemplate(template)

        val updated = template.copy(content = "Updated")
        templateDao.updateTemplate(updated)

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved?.content).isEqualTo("Updated")
    }

    @Test
    fun deleteTemplate_removesTemplate() = runTest {
        val template = createTestTemplate(id = "1")
        templateDao.insertTemplate(template)

        templateDao.deleteTemplate(template)

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteTemplateById_removesUserTemplate() = runTest {
        val template = createTestTemplate(id = "1", isBuiltIn = false)
        templateDao.insertTemplate(template)

        templateDao.deleteTemplateById("1")

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteTemplateById_doesNotRemoveBuiltInTemplate() = runTest {
        val template = createTestTemplate(id = "1", isBuiltIn = true)
        templateDao.insertTemplate(template)

        templateDao.deleteTemplateById("1")

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved).isNotNull() // Still exists
    }

    @Test
    fun incrementUsageCount_increasesCount() = runTest {
        val template = createTestTemplate(id = "1", usageCount = 5)
        templateDao.insertTemplate(template)

        templateDao.incrementUsageCount("1")

        val retrieved = templateDao.getTemplateById("1")
        assertThat(retrieved?.usageCount).isEqualTo(6)
    }

    @Test
    fun getTemplateCount_returnsCorrectCount() = runTest {
        val template1 = createTestTemplate(id = "1")
        val template2 = createTestTemplate(id = "2")

        templateDao.insertTemplate(template1)
        templateDao.insertTemplate(template2)

        val count = templateDao.getTemplateCount()
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun insertTemplates_insertsMultiple() = runTest {
        val templates = listOf(
            createTestTemplate(id = "1"),
            createTestTemplate(id = "2"),
            createTestTemplate(id = "3")
        )

        templateDao.insertTemplates(templates)

        val count = templateDao.getTemplateCount()
        assertThat(count).isEqualTo(3)
    }

    private fun createTestTemplate(
        id: String = "test-id",
        name: String = "Test Template",
        content: String = "Test content",
        description: String? = null,
        variables: String = "[]",
        isBuiltIn: Boolean = false,
        createdAt: Instant = Instant.now(),
        usageCount: Int = 0
    ) = TemplateEntity(
        id = id,
        name = name,
        content = content,
        description = description,
        variables = variables,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt,
        usageCount = usageCount
    )
}
