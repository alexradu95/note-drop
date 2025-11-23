package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TemplateRepository.
 */
@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<Template>> {
        return templateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTemplateById(id: String): Template? {
        return templateDao.getTemplateById(id)?.toDomain()
    }

    override fun getBuiltInTemplates(): Flow<List<Template>> {
        return templateDao.getBuiltInTemplates().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUserTemplates(): Flow<List<Template>> {
        return templateDao.getUserTemplates().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTemplates(query: String): Flow<List<Template>> {
        return templateDao.searchTemplates(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createTemplate(template: Template): Result<Template> {
        return try {
            templateDao.insertTemplate(template.toEntity())
            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTemplate(template: Template): Result<Template> {
        return try {
            templateDao.updateTemplate(template.toEntity())
            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTemplate(id: String): Result<Unit> {
        return try {
            templateDao.deleteTemplateById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementUsageCount(id: String): Result<Unit> {
        return try {
            templateDao.incrementUsageCount(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeBuiltInTemplates(): Result<Unit> {
        return try {
            val count = templateDao.getTemplateCount()
            if (count == 0) {
                val builtInTemplates = Template.builtInTemplates().map { it.toEntity() }
                templateDao.insertTemplates(builtInTemplates)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
