package app.notedrop.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.notedrop.android.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for templates.
 */
@Dao
interface TemplateDao {
    /**
     * Get all templates as a Flow (live updates).
     */
    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, usageCount DESC, name ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    /**
     * Get a single template by ID.
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): TemplateEntity?

    /**
     * Get a single template by ID as Flow.
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    fun getTemplateByIdFlow(id: String): Flow<TemplateEntity?>

    /**
     * Get built-in templates.
     */
    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 ORDER BY name ASC")
    fun getBuiltInTemplates(): Flow<List<TemplateEntity>>

    /**
     * Get user-created templates.
     */
    @Query("SELECT * FROM templates WHERE isBuiltIn = 0 ORDER BY usageCount DESC, name ASC")
    fun getUserTemplates(): Flow<List<TemplateEntity>>

    /**
     * Search templates by name or content.
     */
    @Query("""
        SELECT * FROM templates
        WHERE name LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
        ORDER BY isBuiltIn DESC, usageCount DESC, name ASC
    """)
    fun searchTemplates(query: String): Flow<List<TemplateEntity>>

    /**
     * Insert a new template.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    /**
     * Insert multiple templates (for built-in templates).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<TemplateEntity>)

    /**
     * Update an existing template.
     */
    @Update
    suspend fun updateTemplate(template: TemplateEntity)

    /**
     * Delete a template.
     */
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    /**
     * Delete template by ID (only if not built-in).
     */
    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteTemplateById(id: String)

    /**
     * Increment usage count for a template.
     */
    @Query("UPDATE templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)

    /**
     * Check if any templates exist.
     */
    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getTemplateCount(): Int
}
