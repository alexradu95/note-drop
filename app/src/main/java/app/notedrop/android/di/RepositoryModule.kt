package app.notedrop.android.di

import app.notedrop.android.data.parser.MarkdownParser
import app.notedrop.android.data.parser.MarkdownParserImpl
import app.notedrop.android.data.provider.NoteProvider
import app.notedrop.android.data.provider.ObsidianProvider
import app.notedrop.android.data.provider.filesystem.AndroidFileSystemProvider
import app.notedrop.android.data.provider.filesystem.FileSystemProvider
import app.notedrop.android.data.repository.VaultRepositoryImpl
import app.notedrop.android.domain.repository.VaultRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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

        /**
         * Provide Gson instance for JSON serialization.
         * Used by VaultRepositoryImpl to store vault configurations.
         *
         * Includes custom adapters for:
         * - Instant (Java 8 time) - serialized as epoch millis
         * - ProviderConfig (sealed class) - with type discrimination
         */
        @Provides
        @Singleton
        fun provideGson(): Gson {
            return GsonBuilder()
                .setPrettyPrinting()
                // Instant adapter
                .registerTypeAdapter(java.time.Instant::class.java, object : com.google.gson.JsonSerializer<java.time.Instant>, com.google.gson.JsonDeserializer<java.time.Instant> {
                    override fun serialize(src: java.time.Instant?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
                        return com.google.gson.JsonPrimitive(src?.toEpochMilli() ?: 0)
                    }

                    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): java.time.Instant {
                        return java.time.Instant.ofEpochMilli(json?.asLong ?: 0)
                    }
                })
                // ProviderConfig sealed class adapter
                .registerTypeAdapter(app.notedrop.android.domain.model.ProviderConfig::class.java, object : com.google.gson.JsonSerializer<app.notedrop.android.domain.model.ProviderConfig>, com.google.gson.JsonDeserializer<app.notedrop.android.domain.model.ProviderConfig> {
                    override fun serialize(src: app.notedrop.android.domain.model.ProviderConfig?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
                        if (src == null || context == null) return com.google.gson.JsonNull.INSTANCE

                        val jsonObject = com.google.gson.JsonObject()
                        when (src) {
                            is app.notedrop.android.domain.model.ProviderConfig.LocalConfig -> {
                                jsonObject.addProperty("__type", "LocalConfig")
                                jsonObject.add("data", context.serialize(src, app.notedrop.android.domain.model.ProviderConfig.LocalConfig::class.java))
                            }
                            is app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig -> {
                                jsonObject.addProperty("__type", "ObsidianConfig")
                                jsonObject.add("data", context.serialize(src, app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig::class.java))
                            }
                        }
                        return jsonObject
                    }

                    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): app.notedrop.android.domain.model.ProviderConfig {
                        if (json == null || context == null || !json.isJsonObject) {
                            throw com.google.gson.JsonParseException("Invalid ProviderConfig JSON")
                        }

                        val jsonObject = json.asJsonObject
                        val type = jsonObject.get("__type")?.asString ?: throw com.google.gson.JsonParseException("Missing __type field")
                        val data = jsonObject.get("data") ?: throw com.google.gson.JsonParseException("Missing data field")

                        return when (type) {
                            "LocalConfig" -> context.deserialize(data, app.notedrop.android.domain.model.ProviderConfig.LocalConfig::class.java)
                            "ObsidianConfig" -> context.deserialize(data, app.notedrop.android.domain.model.ProviderConfig.ObsidianConfig::class.java)
                            else -> throw com.google.gson.JsonParseException("Unknown ProviderConfig type: $type")
                        }
                    }
                })
                .create()
        }
    }
}
