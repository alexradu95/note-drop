package app.notedrop.android.util

import app.notedrop.android.domain.model.AppError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError

/**
 * Extension functions for working with kotlin-result's Result type.
 *
 * These helpers make it easier to create and transform Results throughout the codebase.
 */

/**
 * Wraps a suspending block in try-catch and returns Result<T, AppError>.
 *
 * Usage:
 * ```
 * suspend fun saveNote(note: Note): Result<Note, AppError> = resultOf {
 *     noteDao.insert(note.toEntity())
 *     note
 * }
 * ```
 */
inline fun <T> resultOf(block: () -> T): Result<T, AppError> {
    return try {
        Ok(block())
    } catch (e: Exception) {
        Err(AppError.Unknown(e.message ?: "Unknown error", e))
    }
}

/**
 * Suspending version of resultOf.
 */
inline fun <T> suspendResultOf(crossinline block: suspend () -> T): suspend () -> Result<T, AppError> {
    return suspend {
        try {
            Ok(block())
        } catch (e: Exception) {
            Err(AppError.Unknown(e.message ?: "Unknown error", e))
        }
    }
}

/**
 * Wraps a suspending block that can throw exceptions into Result.
 * Specifically for database operations.
 *
 * Usage:
 * ```
 * suspend fun insertNote(note: Note): Result<Note, AppError> = databaseResultOf {
 *     noteDao.insert(note.toEntity())
 *     note
 * }
 * ```
 */
suspend inline fun <T> databaseResultOf(crossinline block: suspend () -> T): Result<T, AppError> {
    return try {
        Ok(block())
    } catch (e: Exception) {
        Err(
            when {
                e.message?.contains("UNIQUE constraint failed") == true ->
                    AppError.Database.ConstraintViolation("Duplicate entry", e)
                e.message?.contains("NOT NULL constraint failed") == true ->
                    AppError.Database.ConstraintViolation("Required field missing", e)
                e.message?.contains("FOREIGN KEY constraint failed") == true ->
                    AppError.Database.ConstraintViolation("Invalid reference", e)
                else ->
                    AppError.Database.InsertError(e.message ?: "Database operation failed", e)
            }
        )
    }
}

/**
 * Wraps a file system operation in try-catch.
 *
 * Usage:
 * ```
 * suspend fun readFile(path: String): Result<String, AppError> = fileSystemResultOf(path) {
 *     File(path).readText()
 * }
 * ```
 */
inline fun <T> fileSystemResultOf(path: String, block: () -> T): Result<T, AppError> {
    return try {
        Ok(block())
    } catch (e: java.io.FileNotFoundException) {
        Err(AppError.FileSystem.NotFound(path))
    } catch (e: java.io.IOException) {
        Err(AppError.FileSystem.ReadError(path, e))
    } catch (e: SecurityException) {
        Err(AppError.FileSystem.PermissionDenied(path, "read"))
    } catch (e: Exception) {
        Err(AppError.Unknown(e.message ?: "File system error", e))
    }
}

/**
 * Maps a nullable value to Result<T, AppError>.
 * Returns Err if value is null.
 *
 * Usage:
 * ```
 * val result: Result<Note, AppError> = note.toResult {
 *     AppError.Database.NotFound("Note", noteId)
 * }
 * ```
 */
inline fun <T> T?.toResult(onNull: () -> AppError): Result<T, AppError> {
    return if (this != null) {
        Ok(this)
    } else {
        Err(onNull())
    }
}

/**
 * Maps a nullable value to Result<T, AppError> with a default NotFound error.
 */
fun <T> T?.toResultOrNotFound(entityType: String, id: String): Result<T, AppError> {
    return toResult { AppError.Database.NotFound(entityType, id) }
}

/**
 * Converts a kotlin.Result to kotlin-result's Result.
 *
 * Usage for legacy code that uses kotlin.Result:
 * ```
 * val kotlinResult: kotlin.Result<Note> = legacyFunction()
 * val result: Result<Note, AppError> = kotlinResult.toAppResult()
 * ```
 */
fun <T> kotlin.Result<T>.toAppResult(): Result<T, AppError> {
    return fold(
        onSuccess = { Ok(it) },
        onFailure = { Err(AppError.Unknown(it.message ?: "Unknown error", it)) }
    )
}

/**
 * Validation helper that checks a condition and returns Result.
 *
 * Usage:
 * ```
 * validate(content.isNotBlank()) {
 *     AppError.Validation.FieldError("content", "Content cannot be blank")
 * }
 * ```
 */
inline fun validate(condition: Boolean, onError: () -> AppError): Result<Unit, AppError> {
    return if (condition) {
        Ok(Unit)
    } else {
        Err(onError())
    }
}

/**
 * Combines multiple validation results.
 * Returns Err with all errors if any failed, Ok(Unit) if all passed.
 *
 * Usage:
 * ```
 * val validations = validateAll(
 *     validate(name.isNotBlank()) { AppError.Validation.MissingField("name") },
 *     validate(path.isNotEmpty()) { AppError.Validation.MissingField("path") }
 * )
 * ```
 */
fun validateAll(vararg results: Result<Unit, AppError>): Result<Unit, AppError> {
    val errors = results.mapNotNull { result ->
        when (result) {
            is Err -> result.error
            is Ok -> null
        }
    }

    return if (errors.isEmpty()) {
        Ok(Unit)
    } else {
        Err(
            when {
                errors.size == 1 -> errors.first()
                else -> AppError.Validation.MultipleErrors(
                    errors.filterIsInstance<AppError.Validation>()
                )
            }
        )
    }
}
