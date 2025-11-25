package app.notedrop.android.data.repository

import app.notedrop.android.data.local.dao.TemplateDao
import app.notedrop.android.data.local.entity.toDomain
import app.notedrop.android.data.local.entity.toEntity
import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Template
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.util.databaseResultOf
import app.notedrop.android.util.toResultOrNotFound
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
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

    override suspend fun getTemplateById(id: String): Result<Template, AppError> {
        return databaseResultOf {
            templateDao.getTemplateById(id)
        }.andThen { entity ->
            entity.toResultOrNotFound("Template", id)
        }.map { entity ->
            entity.toDomain()
        }
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

    override suspend fun createTemplate(template: Template): Result<Template, AppError> {
        return databaseResultOf {
            templateDao.insertTemplate(template.toEntity())
            template
        }
    }

    override suspend fun updateTemplate(template: Template): Result<Template, AppError> {
        return databaseResultOf {
            templateDao.updateTemplate(template.toEntity())
            template
        }
    }

    override suspend fun deleteTemplate(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            templateDao.deleteTemplateById(id)
        }
    }

    override suspend fun incrementUsageCount(id: String): Result<Unit, AppError> {
        return databaseResultOf {
            templateDao.incrementUsageCount(id)
        }
    }

    override suspend fun initializeBuiltInTemplates(): Result<Unit, AppError> {
        return databaseResultOf {
            val count = templateDao.getTemplateCount()
            if (count == 0) {
                val builtInTemplates = Template.builtInTemplates().map { it.toEntity() }
                templateDao.insertTemplates(builtInTemplates)
            }
        }
    }
}
