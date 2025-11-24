package app.notedrop.android.di

import app.notedrop.android.data.parser.MarkdownParser
import app.notedrop.android.data.parser.MarkdownParserImpl
import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.data.provider.ObsidianProvider
import app.notedrop.android.data.provider.filesystem.AndroidFileSystemProvider
import app.notedrop.android.data.provider.filesystem.FileSystemProvider
import app.notedrop.android.data.repository.NoteRepositoryImpl
import app.notedrop.android.data.repository.SyncQueueRepositoryImpl
import app.notedrop.android.data.repository.SyncStateRepositoryImpl
import app.notedrop.android.data.repository.TemplateRepositoryImpl
import app.notedrop.android.data.repository.VaultRepositoryImpl
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.SyncQueueRepository
import app.notedrop.android.domain.repository.SyncStateRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import app.notedrop.android.domain.sync.ConflictResolver
import app.notedrop.android.domain.sync.ConflictResolverImpl
import app.notedrop.android.domain.sync.SyncCoordinator
import app.notedrop.android.domain.sync.SyncCoordinatorImpl
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

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        vaultRepositoryImpl: VaultRepositoryImpl
    ): VaultRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        templateRepositoryImpl: TemplateRepositoryImpl
    ): TemplateRepository

    @Binds
    @Singleton
    abstract fun bindSyncStateRepository(
        syncStateRepositoryImpl: SyncStateRepositoryImpl
    ): SyncStateRepository

    @Binds
    @Singleton
    abstract fun bindSyncQueueRepository(
        syncQueueRepositoryImpl: SyncQueueRepositoryImpl
    ): SyncQueueRepository

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

    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(
        syncCoordinatorImpl: SyncCoordinatorImpl
    ): SyncCoordinator

    @Binds
    @Singleton
    abstract fun bindConflictResolver(
        conflictResolverImpl: ConflictResolverImpl
    ): ConflictResolver

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
