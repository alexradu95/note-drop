package app.notedrop.android.domain.sync

import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.domain.model.ProviderType
import app.notedrop.android.util.FakeNoteProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ProviderFactoryTest {

    private lateinit var obsidianProvider: NoteProvider
    private lateinit var localProvider: NoteProvider
    private lateinit var providerFactory: ProviderFactory

    @Before
    fun setUp() {
        obsidianProvider = FakeNoteProvider()
        localProvider = FakeNoteProvider()
        providerFactory = ProviderFactory(obsidianProvider, localProvider)
    }

    @Test
    fun `getProvider returns obsidian provider for OBSIDIAN type`() {
        // When
        val provider = providerFactory.getProvider(ProviderType.OBSIDIAN)

        // Then
        assertThat(provider).isEqualTo(obsidianProvider)
    }

    @Test
    fun `getProvider returns local provider for LOCAL type`() {
        // When
        val provider = providerFactory.getProvider(ProviderType.LOCAL)

        // Then
        assertThat(provider).isEqualTo(localProvider)
    }

    @Test
    fun `getProvider returns consistent instances`() {
        // When
        val provider1 = providerFactory.getProvider(ProviderType.OBSIDIAN)
        val provider2 = providerFactory.getProvider(ProviderType.OBSIDIAN)

        // Then
        assertThat(provider1).isSameInstanceAs(provider2)
    }
}
