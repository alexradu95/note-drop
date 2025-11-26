package app.notedrop.android.di

import app.notedrop.android.data.parser.MarkdownParser
import app.notedrop.android.data.parser.MarkdownParserImpl
import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.data.provider.ObsidianProvider
import app.notedrop.android.data.provider.filesystem.AndroidFileSystemProvider
import app.notedrop.android.data.provider.filesystem.FileSystemProvider
import app.notedrop.android.data.repository.VaultRepositoryImpl
import app.notedrop.android.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for providing repository and sync dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ========== Repositories ==========
    // VAULT-ONLY: Only keeping VaultRepository for vault configuration
    // Removed: NoteRepository, TemplateRepository, SyncStateRepository, SyncQueueRepository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        vaultRepositoryImpl: VaultRepositoryImpl
    ): VaultRepository

    // ========== Providers ==========

    @Binds
    @Singleton
    @Named("obsidian")
    abstract fun bindObsidianProvider(
        obsidianProvider: ObsidianProvider
    ): NoteProvider

    @Binds
    @Singleton
    @Named("local")
    abstract fun bindLocalProvider(
        localProvider: app.notedrop.android.data.provider.LocalProvider
    ): NoteProvider

    // ========== File System ==========

    @Binds
    @Singleton
    abstract fun bindFileSystemProvider(
        androidFileSystemProvider: AndroidFileSystemProvider
    ): FileSystemProvider

    // ========== Parsers ==========

    @Binds
    @Singleton
    abstract fun bindMarkdownParser(
        markdownParserImpl: MarkdownParserImpl
    ): MarkdownParser

    // ========== Sync Components ==========
    // VAULT-ONLY: Removed sync components - write directly to vault without sync coordination

    companion object {
        /**
         * Provide default NoteProvider (delegates to ObsidianProvider).
         * Used when no specific provider qualifier is requested.
         */
        @Provides
        @Singleton
        fun provideNoteProvider(
            @Named("obsidian") obsidianProvider: NoteProvider
        ): NoteProvider {
            return obsidianProvider
        }
    }
}
