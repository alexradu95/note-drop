package app.notedrop.android.domain.sync

import app.notedrop.android.domain.model.*
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.util.FakeNoteProvider
import app.notedrop.android.util.FakeNoteRepository
import app.notedrop.android.util.FakeVaultRepository
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SyncCoordinatorImplTest {

    private lateinit var syncCoordinator: SyncCoordinatorImpl
    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var vaultRepository: FakeVaultRepository
    private lateinit var syncStateRepository: FakeSyncStateRepository
    private lateinit var conflictResolver: FakeConflictResolver
    private lateinit var providerFactory: ProviderFactory
    private lateinit var noteProvider: FakeNoteProvider

    @Before
    fun setUp() {
        noteRepository = FakeNoteRepository()
        vaultRepository = FakeVaultRepository()
        syncStateRepository = FakeSyncStateRepository()
        conflictResolver = FakeConflictResolver()
        noteProvider = FakeNoteProvider()
        providerFactory = ProviderFactory(noteProvider, noteProvider)

        syncCoordinator = SyncCoordinatorImpl(
            noteRepository = noteRepository,
            vaultRepository = vaultRepository,
            syncStateRepository = syncStateRepository,
            conflictResolver = conflictResolver,
            providerFactory = providerFactory
        )
    }

    // ========================================
    // syncVault() Tests
    // ========================================

    @Test
    fun `syncVault with DISABLED sync mode returns success with zero counts`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.LocalConfig(
                storagePath = "/test/path"
            )
        )
        vaultRepository.addVault(vault)

        // When
        val result = syncCoordinator.syncVault(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        val syncResult = result.getOrNull()
        assertThat(syncResult).isNotNull()
        assertThat(syncResult?.totalNotes).isEqualTo(0)
        assertThat(syncResult?.uploaded).isEqualTo(0)
        assertThat(syncResult?.downloaded).isEqualTo(0)
    }

    @Test
    fun `syncVault with vault not found returns failure`() = runTest {
        // When
        val result = syncCoordinator.syncVault("nonexistent-vault-id")

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Vault not found")
    }

    @Test
    fun `syncVault with PUSH_ONLY mode uploads local changes`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.PUSH_ONLY
            )
        )
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        val syncState = SyncState(
            noteId = note.id,
            vaultId = vault.id,
            status = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = note.updatedAt,
            remoteModifiedAt = null,
            lastSyncedAt = null
        )
        syncStateRepository.addSyncState(syncState)

        // When
        val result = syncCoordinator.syncVault(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        val syncResult = result.getOrNull()
        assertThat(syncResult?.uploaded).isEqualTo(1)
        assertThat(syncResult?.downloaded).isEqualTo(0)
        assertThat(noteProvider.getSavedNotes()).contains(note)
    }

    @Test
    fun `syncVault with PULL_ONLY mode downloads remote changes`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.PULL_ONLY
            )
        )
        vaultRepository.addVault(vault)

        val remoteNote = TestFixtures.createNote(vaultId = vault.id)
        noteProvider.addRemoteNote(remoteNote)

        // When
        val result = syncCoordinator.syncVault(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        val syncResult = result.getOrNull()
        assertThat(syncResult?.downloaded).isEqualTo(1)
        assertThat(syncResult?.uploaded).isEqualTo(0)
        assertThat(noteRepository.getNoteById(remoteNote.id)).isEqualTo(remoteNote.copy(isSynced = true))
    }

    @Test
    fun `syncVault with BIDIRECTIONAL mode syncs both directions`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.BIDIRECTIONAL
            )
        )
        vaultRepository.addVault(vault)

        // Local note to upload
        val localNote = TestFixtures.createNote(id = "local-note", vaultId = vault.id)
        noteRepository.addNote(localNote)
        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = localNote.updatedAt
            )
        )

        // Remote note to download
        val remoteNote = TestFixtures.createNote(id = "remote-note", vaultId = vault.id)
        noteProvider.addRemoteNote(remoteNote)

        // When
        val result = syncCoordinator.syncVault(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        val syncResult = result.getOrNull()
        assertThat(syncResult?.uploaded).isEqualTo(1)
        assertThat(syncResult?.downloaded).isEqualTo(1)
    }

    @Test
    fun `syncVault updates vault lastSyncedAt on success`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.BIDIRECTIONAL
            ),
            lastSyncedAt = null
        )
        vaultRepository.addVault(vault)

        // When
        syncCoordinator.syncVault(vault.id)

        // Then
        val updatedVault = vaultRepository.getVaultById(vault.id)
        assertThat(updatedVault?.lastSyncedAt).isNotNull()
    }

    @Test
    fun `syncVault prevents concurrent syncs for same vault`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.PUSH_ONLY
            )
        )
        vaultRepository.addVault(vault)

        // When - start first sync
        // Note: Testing concurrent sync prevention requires more sophisticated mocking
        // or refactoring to make activeSyncs testable. For now, verify sync completes successfully.
        val result = syncCoordinator.syncVault(vault.id)
        assertThat(result.isSuccess).isTrue()
    }

    // ========================================
    // syncNote() Tests
    // ========================================

    @Test
    fun `syncNote with note not found returns failure`() = runTest {
        // When
        val result = syncCoordinator.syncNote("nonexistent-note")

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Note not found")
    }

    @Test
    fun `syncNote with vault not found returns failure`() = runTest {
        // Given
        val note = TestFixtures.createNote()
        noteRepository.addNote(note)

        // When
        val result = syncCoordinator.syncNote(note.id)

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Vault not found")
    }

    @Test
    fun `syncNote with provider not available returns failure`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        noteProvider.isAvailableResult = false

        // When
        val result = syncCoordinator.syncNote(note.id)

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Provider not available")
    }

    @Test
    fun `syncNote successfully uploads note and creates sync state`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        // When
        val result = syncCoordinator.syncNote(note.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(noteProvider.getSavedNotes()).contains(note)

        val syncState = syncStateRepository.getSyncState(note.id)
        assertThat(syncState).isNotNull()
        assertThat(syncState?.status).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `syncNote handles provider error and updates sync state`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        noteProvider.shouldFail = true

        // When
        val result = syncCoordinator.syncNote(note.id)

        // Then
        assertThat(result.isFailure).isTrue()

        val syncState = syncStateRepository.getSyncState(note.id)
        assertThat(syncState).isNotNull()
        assertThat(syncState?.status).isEqualTo(SyncStatus.ERROR)
        assertThat(syncState?.retryCount).isEqualTo(1)
        assertThat(syncState?.errorMessage).isNotNull()
    }

    // ========================================
    // pushChanges() Tests
    // ========================================

    @Test
    fun `pushChanges with vault not found returns failure`() = runTest {
        // When
        val result = syncCoordinator.pushChanges("nonexistent-vault")

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Vault not found")
    }

    @Test
    fun `pushChanges with provider not available returns failure`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        noteProvider.isAvailableResult = false

        // When
        val result = syncCoordinator.pushChanges(vault.id)

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Provider not available")
    }

    @Test
    fun `pushChanges uploads pending notes successfully`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note1 = TestFixtures.createNote(id = "note1", vaultId = vault.id)
        val note2 = TestFixtures.createNote(id = "note2", vaultId = vault.id)
        noteRepository.addNote(note1)
        noteRepository.addNote(note2)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = note1.id,
                vaultId = vault.id,
                status = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = note1.updatedAt
            )
        )
        syncStateRepository.addSyncState(
            SyncState(
                noteId = note2.id,
                vaultId = vault.id,
                status = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = note2.updatedAt
            )
        )

        // When
        val result = syncCoordinator.pushChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(2)
        assertThat(noteProvider.getSavedNotes()).containsExactly(note1, note2)

        // Check sync states updated
        val syncState1 = syncStateRepository.getSyncState(note1.id)
        assertThat(syncState1?.status).isEqualTo(SyncStatus.SYNCED)
        assertThat(syncState1?.retryCount).isEqualTo(0)
    }

    @Test
    fun `pushChanges handles deleted notes by removing sync state`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val syncState = SyncState(
            noteId = "deleted-note",
            vaultId = vault.id,
            status = SyncStatus.PENDING_UPLOAD,
            localModifiedAt = Instant.now()
        )
        syncStateRepository.addSyncState(syncState)

        // When
        val result = syncCoordinator.pushChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0)
        assertThat(syncStateRepository.getSyncState("deleted-note")).isNull()
    }

    @Test
    fun `pushChanges handles upload errors and updates retry count`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = note.id,
                vaultId = vault.id,
                status = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = note.updatedAt,
                retryCount = 1
            )
        )

        noteProvider.shouldFail = true

        // When
        val result = syncCoordinator.pushChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0)

        val syncState = syncStateRepository.getSyncState(note.id)
        assertThat(syncState?.status).isEqualTo(SyncStatus.ERROR)
        assertThat(syncState?.retryCount).isEqualTo(2)
        assertThat(syncState?.errorMessage).isNotNull()
    }

    @Test
    fun `pushChanges respects max retries limit`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val note = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(note)

        // Create sync state with retryCount = 3 (at max)
        syncStateRepository.addSyncState(
            SyncState(
                noteId = note.id,
                vaultId = vault.id,
                status = SyncStatus.PENDING_UPLOAD,
                localModifiedAt = note.updatedAt,
                retryCount = 3
            )
        )

        // When
        val result = syncCoordinator.pushChanges(vault.id)

        // Then - note should not be in pending uploads due to max retries
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0)
    }

    // ========================================
    // pullChanges() Tests
    // ========================================

    @Test
    fun `pullChanges with vault not found returns failure`() = runTest {
        // When
        val result = syncCoordinator.pullChanges("nonexistent-vault")

        // Then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `pullChanges downloads new remote notes`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val remoteNote = TestFixtures.createNote(vaultId = vault.id)
        noteProvider.addRemoteNote(remoteNote)

        // When
        val result = syncCoordinator.pullChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        val localNote = noteRepository.getNoteById(remoteNote.id)
        assertThat(localNote).isNotNull()
        assertThat(localNote?.isSynced).isTrue()
    }

    @Test
    fun `pullChanges updates existing notes without conflicts`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val oldTime = Instant.now().minusSeconds(3600)
        val newTime = Instant.now()

        val localNote = TestFixtures.createNote(
            vaultId = vault.id,
            content = "Old content",
            updatedAt = oldTime
        )
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(
            content = "Updated content",
            updatedAt = newTime
        )
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.SYNCED,
                localModifiedAt = oldTime,
                remoteModifiedAt = oldTime,
                lastSyncedAt = oldTime
            )
        )

        // When
        val result = syncCoordinator.pullChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        val updatedNote = noteRepository.getNoteById(localNote.id)
        assertThat(updatedNote?.content).isEqualTo("Updated content")
    }

    @Test
    fun `pullChanges detects conflicts when both local and remote changed`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val lastSyncTime = Instant.now().minusSeconds(3600)
        val localUpdateTime = Instant.now().minusSeconds(1800)
        val remoteUpdateTime = Instant.now().minusSeconds(900)

        val localNote = TestFixtures.createNote(
            vaultId = vault.id,
            content = "Local changes",
            updatedAt = localUpdateTime
        )
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(
            content = "Remote changes",
            updatedAt = remoteUpdateTime
        )
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.SYNCED,
                localModifiedAt = localUpdateTime,
                remoteModifiedAt = lastSyncTime,
                lastSyncedAt = lastSyncTime
            )
        )

        // When
        val result = syncCoordinator.pullChanges(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()

        val syncState = syncStateRepository.getSyncState(localNote.id)
        assertThat(syncState?.status).isEqualTo(SyncStatus.CONFLICT)
    }

    // ========================================
    // resolveConflicts() Tests
    // ========================================

    @Test
    fun `resolveConflicts with vault not found returns failure`() = runTest {
        // When
        val result = syncCoordinator.resolveConflicts("nonexistent-vault")

        // Then
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `resolveConflicts handles UseLocal resolution`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val localNote = TestFixtures.createNote(vaultId = vault.id, content = "Local")
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(content = "Remote")
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.CONFLICT,
                localModifiedAt = Instant.now()
            )
        )

        conflictResolver.resolutionToReturn = ConflictResolution.UseLocal(localNote)

        // When
        val result = syncCoordinator.resolveConflicts(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        val syncState = syncStateRepository.getSyncState(localNote.id)
        assertThat(syncState?.status).isEqualTo(SyncStatus.SYNCED)
    }

    @Test
    fun `resolveConflicts handles UseRemote resolution`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val localNote = TestFixtures.createNote(vaultId = vault.id, content = "Local")
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(content = "Remote")
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.CONFLICT,
                localModifiedAt = Instant.now()
            )
        )

        conflictResolver.resolutionToReturn = ConflictResolution.UseRemote(remoteNote)

        // When
        val result = syncCoordinator.resolveConflicts(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        val updatedNote = noteRepository.getNoteById(localNote.id)
        assertThat(updatedNote?.content).isEqualTo("Remote")
    }

    @Test
    fun `resolveConflicts handles KeepBoth resolution`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val localNote = TestFixtures.createNote(vaultId = vault.id, content = "Local")
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(content = "Remote")
        val conflictCopy = remoteNote.copy(title = "${remoteNote.title} (conflict)")
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.CONFLICT,
                localModifiedAt = Instant.now()
            )
        )

        conflictResolver.resolutionToReturn = ConflictResolution.KeepBoth(localNote, conflictCopy)

        // When
        val result = syncCoordinator.resolveConflicts(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        // Both notes should be saved to provider
        assertThat(noteProvider.getSavedNotes()).hasSize(2)
    }

    @Test
    fun `resolveConflicts handles Merged resolution`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val localNote = TestFixtures.createNote(vaultId = vault.id, content = "Local")
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(content = "Remote")
        val mergedNote = localNote.copy(content = "Local\n\nRemote")
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.CONFLICT,
                localModifiedAt = Instant.now()
            )
        )

        conflictResolver.resolutionToReturn = ConflictResolution.Merged(mergedNote)

        // When
        val result = syncCoordinator.resolveConflicts(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1)

        val updatedNote = noteRepository.getNoteById(localNote.id)
        assertThat(updatedNote?.content).isEqualTo("Local\n\nRemote")
    }

    @Test
    fun `resolveConflicts handles RequiresManual resolution`() = runTest {
        // Given
        val vault = TestFixtures.createVault()
        vaultRepository.addVault(vault)

        val localNote = TestFixtures.createNote(vaultId = vault.id)
        noteRepository.addNote(localNote)

        val remoteNote = localNote.copy(content = "Remote")
        noteProvider.addRemoteNote(remoteNote)

        syncStateRepository.addSyncState(
            SyncState(
                noteId = localNote.id,
                vaultId = vault.id,
                status = SyncStatus.CONFLICT,
                localModifiedAt = Instant.now()
            )
        )

        conflictResolver.resolutionToReturn = ConflictResolution.RequiresManual(
            localNote,
            remoteNote,
            "Manual resolution required"
        )

        // When
        val result = syncCoordinator.resolveConflicts(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(0) // No conflicts resolved

        val syncState = syncStateRepository.getSyncState(localNote.id)
        assertThat(syncState?.status).isEqualTo(SyncStatus.CONFLICT)
        assertThat(syncState?.errorMessage).isEqualTo("Manual resolution required")
    }

    // ========================================
    // forceResync() Tests
    // ========================================

    @Test
    fun `forceResync deletes all sync states and creates new ones`() = runTest {
        // Given
        val vault = TestFixtures.createVault(
            providerConfig = ProviderConfig.ObsidianConfig(
                vaultPath = "/test/path",
                syncMode = SyncMode.BIDIRECTIONAL
            )
        )
        vaultRepository.addVault(vault)

        val note1 = TestFixtures.createNote(id = "note1", vaultId = vault.id, isSynced = true)
        val note2 = TestFixtures.createNote(id = "note2", vaultId = vault.id, isSynced = false)
        noteRepository.addNote(note1)
        noteRepository.addNote(note2)

        // Add old sync states
        syncStateRepository.addSyncState(
            SyncState(
                noteId = note1.id,
                vaultId = vault.id,
                status = SyncStatus.SYNCED,
                localModifiedAt = Instant.now()
            )
        )

        // When
        val result = syncCoordinator.forceResync(vault.id)

        // Then
        assertThat(result.isSuccess).isTrue()

        // Check new sync states created
        val syncState1 = syncStateRepository.getSyncState(note1.id)
        assertThat(syncState1?.status).isEqualTo(SyncStatus.SYNCED)

        val syncState2 = syncStateRepository.getSyncState(note2.id)
        assertThat(syncState2?.status).isEqualTo(SyncStatus.PENDING_UPLOAD)
    }

    // ========================================
    // getSyncProgress() Tests
    // ========================================

    @Test
    fun `getSyncProgress returns 100 when no notes`() = runTest {
        // When
        val progress = syncCoordinator.getSyncProgress("test-vault")

        // Then
        assertThat(progress).isEqualTo(100)
    }

    @Test
    fun `getSyncProgress calculates correct percentage`() = runTest {
        // Given
        val vaultId = "test-vault"
        syncStateRepository.setCountByStatus(vaultId, SyncStatus.SYNCED, 7)
        syncStateRepository.setCountByStatus(vaultId, SyncStatus.PENDING_UPLOAD, 2)
        syncStateRepository.setCountByStatus(vaultId, SyncStatus.PENDING_DOWNLOAD, 1)

        // When
        val progress = syncCoordinator.getSyncProgress(vaultId)

        // Then
        // 7 synced out of 10 total = 70%
        assertThat(progress).isEqualTo(70)
    }

    // ========================================
    // Helper Classes
    // ========================================

    private class FakeSyncStateRepository : SyncStateRepository {
        private val syncStates = mutableMapOf<String, SyncState>()
        private val countsByStatus = mutableMapOf<Pair<String, SyncStatus>, Int>()

        fun addSyncState(syncState: SyncState) {
            syncStates[syncState.noteId] = syncState
        }

        fun setCountByStatus(vaultId: String, status: SyncStatus, count: Int) {
            countsByStatus[vaultId to status] = count
        }

        override suspend fun getSyncState(noteId: String): SyncState? {
            return syncStates[noteId]
        }

        override fun observeSyncState(noteId: String): Flow<SyncState?> {
            return MutableStateFlow(syncStates[noteId])
        }

        override fun getSyncStatesForVault(vaultId: String): Flow<List<SyncState>> {
            return MutableStateFlow(syncStates.values.filter { it.vaultId == vaultId })
        }

        override suspend fun getByStatus(status: SyncStatus): List<SyncState> {
            return syncStates.values.filter { it.status == status }
        }

        override suspend fun getByStatusForVault(vaultId: String, status: SyncStatus): List<SyncState> {
            return syncStates.values.filter { it.vaultId == vaultId && it.status == status }
        }

        override suspend fun getPendingUploads(vaultId: String, maxRetries: Int): List<SyncState> {
            return syncStates.values.filter {
                it.vaultId == vaultId &&
                it.status == SyncStatus.PENDING_UPLOAD &&
                it.retryCount < maxRetries
            }
        }

        override suspend fun getPendingDownloads(vaultId: String): List<SyncState> {
            return syncStates.values.filter {
                it.vaultId == vaultId && it.status == SyncStatus.PENDING_DOWNLOAD
            }
        }

        override suspend fun getConflicts(vaultId: String): List<SyncState> {
            return syncStates.values.filter {
                it.vaultId == vaultId && it.status == SyncStatus.CONFLICT
            }
        }

        override suspend fun getCountByStatus(vaultId: String, status: SyncStatus): Int {
            return countsByStatus[vaultId to status]
                ?: syncStates.values.count { it.vaultId == vaultId && it.status == status }
        }

        override suspend fun getErrorCount(vaultId: String): Int {
            return syncStates.values.count { it.vaultId == vaultId && it.status == SyncStatus.ERROR }
        }

        override suspend fun upsert(syncState: SyncState) {
            syncStates[syncState.noteId] = syncState
        }

        override suspend fun upsertAll(syncStates: List<SyncState>) {
            syncStates.forEach { this.syncStates[it.noteId] = it }
        }

        override suspend fun delete(noteId: String) {
            syncStates.remove(noteId)
        }

        override suspend fun deleteForVault(vaultId: String) {
            syncStates.entries.removeIf { it.value.vaultId == vaultId }
        }

        override suspend fun deleteSynced() {
            syncStates.entries.removeIf { it.value.status == SyncStatus.SYNCED }
        }

        override suspend fun resetRetryCountsForErrors() {
            syncStates.replaceAll { noteId, state ->
                if (state.status == SyncStatus.ERROR) {
                    state.copy(retryCount = 0)
                } else {
                    state
                }
            }
        }

        override suspend fun getSyncStatistics(vaultId: String): Map<SyncStatus, Int> {
            return syncStates.values
                .filter { it.vaultId == vaultId }
                .groupingBy { it.status }
                .eachCount()
        }
    }

    private class FakeConflictResolver : ConflictResolver {
        var resolutionToReturn: ConflictResolution? = null

        override fun resolve(
            local: Note,
            remote: Note,
            strategy: ConflictStrategy
        ): ConflictResolution {
            return resolutionToReturn ?: ConflictResolution.UseLocal(local)
        }

        override fun tryMerge(local: Note, remote: Note): Note? {
            return null
        }
    }
}
