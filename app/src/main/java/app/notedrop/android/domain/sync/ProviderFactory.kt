package app.notedrop.android.domain.sync

import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.domain.model.ProviderType
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Factory for creating NoteProvider instances based on provider type.
 * This allows the sync coordinator to work with any provider type.
 */
@Singleton
class ProviderFactory @Inject constructor(
    @Named("obsidian") private val obsidianProvider: NoteProvider,
    @Named("local") private val localProvider: NoteProvider
    // Add more providers as they're implemented
) {

    /**
     * Get the appropriate provider for a given provider type.
     */
    fun getProvider(type: ProviderType): NoteProvider {
        return when (type) {
            ProviderType.OBSIDIAN -> obsidianProvider
            ProviderType.LOCAL -> localProvider
            ProviderType.NOTION -> throw NotImplementedError("Notion provider not yet implemented")
            ProviderType.CAPACITIES -> throw NotImplementedError("Capacities provider not yet implemented")
            ProviderType.CUSTOM -> throw NotImplementedError("Custom provider not yet implemented")
        }
    }
}
