package app.notedrop.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.notedrop.android.domain.model.Template
import java.time.Instant

/**
 * Room entity for storing templates in the local database.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val content: String,
    val description: String?,
    val variables: String, // Stored as comma-separated values
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val usageCount: Int
)

/**
 * Convert domain Template to database TemplateEntity.
 */
fun Template.toEntity(): TemplateEntity {
    return TemplateEntity(
        id = id,
        name = name,
        content = content,
        description = description,
        variables = variables.joinToString(","),
        isBuiltIn = isBuiltIn,
        createdAt = createdAt.toEpochMilli(),
        usageCount = usageCount
    )
}

/**
 * Convert database TemplateEntity to domain Template.
 */
fun TemplateEntity.toDomain(): Template {
    return Template(
        id = id,
        name = name,
        content = content,
        description = description,
        variables = if (variables.isEmpty()) emptyList() else variables.split(","),
        isBuiltIn = isBuiltIn,
        createdAt = Instant.ofEpochMilli(createdAt),
        usageCount = usageCount
    )
}
