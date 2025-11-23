package app.notedrop.android.domain.repository

import app.notedrop.android.domain.model.Template
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for template operations.
 */
interface TemplateRepository {
    /**
     * Get all templates as a Flow.
     */
    fun getAllTemplates(): Flow<List<Template>>

    /**
     * Get a single template by ID.
     */
    suspend fun getTemplateById(id: String): Template?

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
    suspend fun createTemplate(template: Template): Result<Template>

    /**
     * Update an existing template.
     */
    suspend fun updateTemplate(template: Template): Result<Template>

    /**
     * Delete a template (only user-created).
     */
    suspend fun deleteTemplate(id: String): Result<Unit>

    /**
     * Increment usage count for a template.
     */
    suspend fun incrementUsageCount(id: String): Result<Unit>

    /**
     * Initialize built-in templates if not present.
     */
    suspend fun initializeBuiltInTemplates(): Result<Unit>
}
