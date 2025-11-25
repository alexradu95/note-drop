package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.AppError
import app.notedrop.android.domain.model.Template
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for template operations.
 *
 * All mutating operations return Result<T, AppError> for type-safe error handling.
 */
interface TemplateRepository {
    /**
     * Get all templates as a Flow.
     */
    fun getAllTemplates(): Flow<List<Template>>

    /**
     * Get a single template by ID.
     */
    suspend fun getTemplateById(id: String): Result<Template, AppError>

    /**
     * Get built-in templates.
     */
    fun getBuiltInTemplates(): Flow<List<Template>>

    /**
     * Get user-created templates.
     */
    fun getUserTemplates(): Flow<List<Template>>

    /**
     * Search templates by query.
     */
    fun searchTemplates(query: String): Flow<List<Template>>

    /**
     * Create a new template.
     */
    suspend fun createTemplate(template: Template): Result<Template, AppError>

    /**
     * Update an existing template.
     */
    suspend fun updateTemplate(template: Template): Result<Template, AppError>

    /**
     * Delete a template (only user-created).
     */
    suspend fun deleteTemplate(id: String): Result<Unit, AppError>

    /**
     * Increment usage count for a template.
     */
    suspend fun incrementUsageCount(id: String): Result<Unit, AppError>

    /**
     * Initialize built-in templates if not present.
     */
    suspend fun initializeBuiltInTemplates(): Result<Unit, AppError>
}
