package app.notedrop.android.di

import app.notedrop.android.data.repository.NoteRepositoryImpl
import app.notedrop.android.data.repository.TemplateRepositoryImpl
import app.notedrop.android.data.repository.VaultRepositoryImpl
import app.notedrop.android.domain.repository.NoteRepository
import app.notedrop.android.domain.repository.TemplateRepository
import app.notedrop.android.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

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
}
