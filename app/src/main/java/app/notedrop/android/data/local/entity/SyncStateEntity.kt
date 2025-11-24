package app.notedrop.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.notedrop.android.domain.model.SyncState
import app.notedrop.android.domain.model.SyncStatus
import java.time.Instant

/**
 * Room entity for storing synchronization state.
 * Provider-agnostic - works with any storage backend.
 */
@Entity(tableName = "sync_states")
data class SyncStateEntity(
    @PrimaryKey
    val noteId: String,
    val vaultId: String,
    val status: SyncStatus,
    val localModifiedAt: Long,
    val remoteModifiedAt: Long?,
    val lastSyncedAt: Long?,
    val remotePath: String?,
    val retryCount: Int,
    val errorMessage: String?
)

/**
 * Convert domain model to entity.
 */
fun SyncState.toEntity(): SyncStateEntity {
    return SyncStateEntity(
        noteId = noteId,
        vaultId = vaultId,
        status = status,
        localModifiedAt = localModifiedAt.toEpochMilli(),
        remoteModifiedAt = remoteModifiedAt?.toEpochMilli(),
        lastSyncedAt = lastSyncedAt?.toEpochMilli(),
        remotePath = remotePath,
        retryCount = retryCount,
        errorMessage = errorMessage
    )
}

/**
 * Convert entity to domain model.
 */
fun SyncStateEntity.toDomain(): SyncState {
    return SyncState(
        noteId = noteId,
        vaultId = vaultId,
        status = status,
        localModifiedAt = Instant.ofEpochMilli(localModifiedAt),
        remoteModifiedAt = remoteModifiedAt?.let { Instant.ofEpochMilli(it) },
        lastSyncedAt = lastSyncedAt?.let { Instant.ofEpochMilli(it) },
        remotePath = remotePath,
        retryCount = retryCount,
        errorMessage = errorMessage
    )
}
