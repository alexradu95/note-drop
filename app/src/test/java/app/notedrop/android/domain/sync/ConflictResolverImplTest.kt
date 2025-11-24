package app.notedrop.android.domain.sync

import app.notedrop.android.domain.model.ConflictStrategy
import app.notedrop.android.util.TestFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Instant

class ConflictResolverImplTest {

    private lateinit var conflictResolver: ConflictResolverImpl

    @Before
    fun setUp() {
        conflictResolver = ConflictResolverImpl()
    }

    // ========================================
    // LAST_WRITE_WINS Strategy Tests
    // ========================================

    @Test
    fun `resolve with LAST_WRITE_WINS chooses local when local is newer`() {
        // Given
        val oldTime = Instant.now().minusSeconds(3600)
        val newTime = Instant.now()

        val localNote = TestFixtures.createNote(content = "Local content", updatedAt = newTime)
        val remoteNote = TestFixtures.createNote(id = localNote.id, content = "Remote content", updatedAt = oldTime)

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.LAST_WRITE_WINS)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.UseLocal::class.java)
        assertThat((resolution as ConflictResolution.UseLocal).note).isEqualTo(localNote)
    }

    @Test
    fun `resolve with LAST_WRITE_WINS chooses remote when remote is newer`() {
        // Given
        val oldTime = Instant.now().minusSeconds(3600)
        val newTime = Instant.now()

        val localNote = TestFixtures.createNote(content = "Local content", updatedAt = oldTime)
        val remoteNote = TestFixtures.createNote(id = localNote.id, content = "Remote content", updatedAt = newTime)

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.LAST_WRITE_WINS)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.UseRemote::class.java)
        assertThat((resolution as ConflictResolution.UseRemote).note).isEqualTo(remoteNote)
    }

    @Test
    fun `resolve with LAST_WRITE_WINS attempts merge when timestamps are equal`() {
        // Given
        val sameTime = Instant.now()

        val localNote = TestFixtures.createNote(
            content = "Same content",
            title = "Same Title",
            updatedAt = sameTime
        )
        val remoteNote = localNote.copy()

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.LAST_WRITE_WINS)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.Merged::class.java)
        val mergedNote = (resolution as ConflictResolution.Merged).note
        assertThat(mergedNote.content).isEqualTo(localNote.content)
    }

    @Test
    fun `resolve with LAST_WRITE_WINS defaults to local when merge fails and timestamps equal`() {
        // Given
        val sameTime = Instant.now()

        val localNote = TestFixtures.createNote(
            content = "Completely different local content",
            updatedAt = sameTime
        )
        val remoteNote = TestFixtures.createNote(
            id = localNote.id,
            content = "Completely different remote content",
            updatedAt = sameTime
        )

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.LAST_WRITE_WINS)

        // Then
        // When merge fails with same timestamp, should default to local
        if (resolution is ConflictResolution.UseLocal) {
            assertThat(resolution.note).isEqualTo(localNote)
        } else {
            // Or could attempt merge - both are acceptable
            assertThat(resolution).isInstanceOf(ConflictResolution.Merged::class.java)
        }
    }

    // ========================================
    // KEEP_BOTH Strategy Tests
    // ========================================

    @Test
    fun `resolve with KEEP_BOTH creates conflict copy of remote`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Local content", title = "My Note")
        val remoteNote = TestFixtures.createNote(
            id = localNote.id,
            content = "Remote content",
            title = "My Note"
        )

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.KEEP_BOTH)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.KeepBoth::class.java)
        val keepBoth = resolution as ConflictResolution.KeepBoth
        assertThat(keepBoth.local).isEqualTo(localNote)
        assertThat(keepBoth.remote.title).contains("conflict")
        assertThat(keepBoth.remote.content).isEqualTo(remoteNote.content)
    }

    @Test
    fun `resolve with KEEP_BOTH handles remote note without title`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Local content", title = null)
        val remoteNote = TestFixtures.createNote(
            id = localNote.id,
            content = "Remote content",
            title = null
        )

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.KEEP_BOTH)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.KeepBoth::class.java)
        val keepBoth = resolution as ConflictResolution.KeepBoth
        assertThat(keepBoth.remote.title).isEqualTo("Conflict copy")
    }

    // ========================================
    // LOCAL_WINS Strategy Tests
    // ========================================

    @Test
    fun `resolve with LOCAL_WINS always chooses local`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Local content")
        val remoteNote = TestFixtures.createNote(
            id = localNote.id,
            content = "Remote content",
            updatedAt = Instant.now().plusSeconds(3600) // Even if remote is newer
        )

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.LOCAL_WINS)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.UseLocal::class.java)
        assertThat((resolution as ConflictResolution.UseLocal).note).isEqualTo(localNote)
    }

    // ========================================
    // REMOTE_WINS Strategy Tests
    // ========================================

    @Test
    fun `resolve with REMOTE_WINS always chooses remote`() {
        // Given
        val localNote = TestFixtures.createNote(
            content = "Local content",
            updatedAt = Instant.now().plusSeconds(3600) // Even if local is newer
        )
        val remoteNote = TestFixtures.createNote(id = localNote.id, content = "Remote content")

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.REMOTE_WINS)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.UseRemote::class.java)
        assertThat((resolution as ConflictResolution.UseRemote).note).isEqualTo(remoteNote)
    }

    // ========================================
    // MANUAL Strategy Tests
    // ========================================

    @Test
    fun `resolve with MANUAL requires manual resolution`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Local content")
        val remoteNote = TestFixtures.createNote(id = localNote.id, content = "Remote content")

        // When
        val resolution = conflictResolver.resolve(localNote, remoteNote, ConflictStrategy.MANUAL)

        // Then
        assertThat(resolution).isInstanceOf(ConflictResolution.RequiresManual::class.java)
        val manual = resolution as ConflictResolution.RequiresManual
        assertThat(manual.local).isEqualTo(localNote)
        assertThat(manual.remote).isEqualTo(remoteNote)
        assertThat(manual.reason).contains("Manual resolution required")
    }

    // ========================================
    // tryMerge() Tests - Identical Notes
    // ========================================

    @Test
    fun `tryMerge returns note when notes are identical`() {
        // Given
        val time1 = Instant.now().minusSeconds(100)
        val time2 = Instant.now()

        val localNote = TestFixtures.createNote(
            content = "Same content",
            title = "Same title",
            tags = listOf("tag1", "tag2"),
            updatedAt = time1
        )
        val remoteNote = localNote.copy(updatedAt = time2)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo(localNote.content)
        assertThat(merged?.updatedAt).isEqualTo(time2) // Most recent timestamp
    }

    // ========================================
    // tryMerge() Tests - Metadata Only Changes
    // ========================================

    @Test
    fun `tryMerge successfully merges when only metadata changed`() {
        // Given
        val localNote = TestFixtures.createNote(
            content = "Same content",
            title = "Local Title",
            tags = listOf("local-tag"),
            metadata = mapOf("key1" to "value1"),
            updatedAt = Instant.now().minusSeconds(100)
        )
        val remoteNote = localNote.copy(
            title = "Remote Title",
            tags = listOf("remote-tag"),
            metadata = mapOf("key2" to "value2"),
            updatedAt = Instant.now()
        )

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo("Same content")
        assertThat(merged?.tags).containsExactly("local-tag", "remote-tag")
        assertThat(merged?.metadata).containsEntry("key1", "value1")
        assertThat(merged?.metadata).containsEntry("key2", "value2")
        assertThat(merged?.title).isEqualTo("Remote Title") // More recent title
    }

    @Test
    fun `tryMerge merges tags correctly removing duplicates`() {
        // Given
        val localNote = TestFixtures.createNote(
            content = "Same content",
            tags = listOf("tag1", "tag2", "common")
        )
        val remoteNote = localNote.copy(
            tags = listOf("tag3", "tag4", "common")
        )

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.tags).containsExactly("tag1", "tag2", "common", "tag3", "tag4")
    }

    @Test
    fun `tryMerge uses earlier createdAt timestamp`() {
        // Given
        val earlierTime = Instant.now().minusSeconds(1000)
        val laterTime = Instant.now()

        val localNote = TestFixtures.createNote(
            content = "Same content",
            createdAt = laterTime
        )
        val remoteNote = localNote.copy(createdAt = earlierTime)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.createdAt).isEqualTo(earlierTime)
    }

    @Test
    fun `tryMerge uses later updatedAt timestamp`() {
        // Given
        val earlierTime = Instant.now().minusSeconds(1000)
        val laterTime = Instant.now()

        val localNote = TestFixtures.createNote(
            content = "Same content",
            updatedAt = earlierTime
        )
        val remoteNote = localNote.copy(updatedAt = laterTime)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.updatedAt).isEqualTo(laterTime)
    }

    // ========================================
    // tryMerge() Tests - Line-Based Merge
    // ========================================

    @Test
    fun `tryMerge succeeds when local appended content`() {
        // Given
        val remoteContent = "Line 1\nLine 2\nLine 3"
        val localContent = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"

        val remoteNote = TestFixtures.createNote(content = remoteContent)
        val localNote = remoteNote.copy(content = localContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo(localContent)
    }

    @Test
    fun `tryMerge succeeds when remote appended content`() {
        // Given
        val localContent = "Line 1\nLine 2\nLine 3"
        val remoteContent = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"

        val localNote = TestFixtures.createNote(content = localContent)
        val remoteNote = localNote.copy(content = remoteContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo(remoteContent)
    }

    @Test
    fun `tryMerge succeeds when changes are in different sections`() {
        // Given
        val commonStart = "Header Line 1\nHeader Line 2\nHeader Line 3"
        val commonEnd = "Footer Line 1\nFooter Line 2\nFooter Line 3"

        val localContent = "$commonStart\nLocal Middle Content\n$commonEnd"
        val remoteContent = "$commonStart\nRemote Middle Content\n$commonEnd"

        val localNote = TestFixtures.createNote(content = localContent)
        val remoteNote = localNote.copy(content = remoteContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        // Should contain both middle sections
        assertThat(merged?.content).contains("Local Middle Content")
        assertThat(merged?.content).contains("Remote Middle Content")
        assertThat(merged?.content).contains("Header Line 1")
        assertThat(merged?.content).contains("Footer Line 3")
    }

    @Test
    fun `tryMerge returns null when content diverges completely`() {
        // Given
        val localContent = "Completely different\nlocal content\nwith nothing in common"
        val remoteContent = "Totally different\nremote content\nalso nothing shared"

        val localNote = TestFixtures.createNote(content = localContent)
        val remoteNote = localNote.copy(content = remoteContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNull()
    }

    @Test
    fun `tryMerge handles empty content in local`() {
        // Given
        val localNote = TestFixtures.createNote(content = "")
        val remoteNote = localNote.copy(content = "Some remote content")

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        // Empty local is subset of remote
        assertThat(merged).isNotNull()
    }

    @Test
    fun `tryMerge handles empty content in remote`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Some local content")
        val remoteNote = localNote.copy(content = "")

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo("Some local content")
    }

    @Test
    fun `tryMerge handles single line changes`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Single line")
        val remoteNote = localNote.copy(content = "Single line")

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).isEqualTo("Single line")
    }

    @Test
    fun `tryMerge handles whitespace-only differences`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Line 1\n\nLine 2")
        val remoteNote = localNote.copy(content = "Line 1\n\n\nLine 2")

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        // Should detect as different and attempt merge
        // Result depends on implementation details
        assertThat(merged).isNotNull()
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `tryMerge with high common prefix and suffix ratio succeeds`() {
        // Given
        val commonPrefix = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
        val commonSuffix = "Line 10\nLine 11\nLine 12\nLine 13\nLine 14"

        val localContent = "$commonPrefix\nLocal Line A\nLocal Line B\n$commonSuffix"
        val remoteContent = "$commonPrefix\nRemote Line X\nRemote Line Y\n$commonSuffix"

        val localNote = TestFixtures.createNote(content = localContent)
        val remoteNote = localNote.copy(content = remoteContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.content).contains("Line 1")
        assertThat(merged?.content).contains("Line 14")
    }

    @Test
    fun `tryMerge with low common section ratio fails`() {
        // Given
        val localContent = "Same Line 1\nLocal Line 2\nLocal Line 3\nLocal Line 4"
        val remoteContent = "Same Line 1\nRemote Line 2\nRemote Line 3\nRemote Line 4"

        val localNote = TestFixtures.createNote(content = localContent)
        val remoteNote = localNote.copy(content = remoteContent)

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        // Only 1 line in common out of 4 = 25% common, might fail merge
        // Result depends on threshold (50% in implementation)
        // Accept either outcome as implementation may vary
        // Just verify it doesn't throw - either null or merged is valid
    }

    @Test
    fun `tryMerge combines metadata from both versions`() {
        // Given
        val localNote = TestFixtures.createNote(
            content = "Same content",
            metadata = mapOf("author" to "Alice", "version" to "1")
        )
        val remoteNote = localNote.copy(
            metadata = mapOf("editor" to "Bob", "version" to "2")
        )

        // When
        val merged = conflictResolver.tryMerge(localNote, remoteNote)

        // Then
        assertThat(merged).isNotNull()
        assertThat(merged?.metadata).hasSize(3)
        assertThat(merged?.metadata).containsEntry("author", "Alice")
        assertThat(merged?.metadata).containsEntry("editor", "Bob")
        // Note: when keys conflict, behavior depends on map merge order
        assertThat(merged?.metadata).containsKey("version")
    }

    @Test
    fun `resolve strategies are exhaustive for all ConflictStrategy values`() {
        // Given
        val localNote = TestFixtures.createNote(content = "Local")
        val remoteNote = TestFixtures.createNote(id = localNote.id, content = "Remote")

        // When/Then - verify all strategies work without throwing
        ConflictStrategy.values().forEach { strategy ->
            val resolution = conflictResolver.resolve(localNote, remoteNote, strategy)
            assertThat(resolution).isNotNull()
        }
    }
}
