# NoteDrop Architecture - Modular Integration Layer

## Overview

NoteDrop implements a **modular, provider-agnostic architecture** for note synchronization. This design allows seamless integration with multiple note-taking platforms while maintaining a clean separation of concerns.

## Supported Providers

### Current
- **Local** - Simple folder with txt/md files (no cloud sync)
- **Obsidian** - First external provider (in progress)

### Planned
- **Notion** - API-based workspace integration
- **Capacities** - API-based workspace integration
- **Custom** - Extensible for any future provider

## Architecture Layers

```
┌──────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
│  (Compose, Widgets, Settings)                               │
└────────────────────┬─────────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────────┐
│                 Domain Layer                                 │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      SyncCoordinator (Provider-Agnostic)             │  │
│  │  - Orchestrates all sync operations                  │  │
│  │  - Works with any NoteProvider                       │  │
│  │  - Handles conflict detection                        │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │      SyncState & Models                               │  │
│  │  - SyncStatus, ConflictStrategy, SyncMode            │  │
│  │  - FileMetadata, NoteMetadata                        │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬─────────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────────┐
│                  Data Layer                                  │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │     NoteProvider Interface (Abstract)                 │  │
│  │  - saveNote(), loadNote(), deleteNote()              │  │
│  │  - listNotes(), watchChanges()                       │  │
│  │  - getMetadata(), resolveConflict()                  │  │
│  └─────┬──────────────────────────────┬─────────────────┘  │
│        │                               │                     │
│   ┌────▼───────────┐            ┌─────▼────────────┐       │
│   │ LocalProvider  │            │ ObsidianProvider │       │
│   │ NotionProvider │            │ CapacitiesProvider│      │
│   └────┬───────────┘            └─────┬────────────┘       │
│        │                               │                     │
│   ┌────▼───────────────────────────────▼──────────────┐    │
│   │   FileSystemProvider (Generic File Operations)    │    │
│   │  - Used by all file-based providers               │    │
│   │  - readFile(), writeFile(), watchDirectory()      │    │
│   └───────────────────────────────────────────────────┘    │
│                                                              │
│   ┌──────────────────────────────────────────────────┐     │
│   │   MarkdownParser (Generic Markdown Parsing)      │     │
│   │  - parse(), serialize()                          │     │
│   │  - extractFrontmatter(), extractLinks()          │     │
│   └──────────────────────────────────────────────────┘     │
│                                                              │
│   ┌──────────────────────────────────────────────────┐     │
│   │   SyncStateRepository                             │     │
│   │  - Track sync state per note                     │     │
│   │  - Room database persistence                     │     │
│   └──────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Domain Models (Provider-Agnostic)

#### **SyncState**
```kotlin
data class SyncState(
    val noteId: String,
    val vaultId: String,
    val status: SyncStatus,
    val localModifiedAt: Instant,
    val remoteModifiedAt: Instant?,
    val lastSyncedAt: Instant?,
    val remotePath: String?,
    val retryCount: Int,
    val errorMessage: String?
)
```

**Status Types:**
- `PENDING_UPLOAD` - Local changes need to be pushed
- `PENDING_DOWNLOAD` - Remote changes need to be pulled
- `SYNCED` - Local and remote are identical
- `CONFLICT` - Both have changes
- `ERROR` - Sync failed
- `NEVER_SYNCED` - Initial state

#### **ConflictStrategy**
- `LAST_WRITE_WINS` - Newer timestamp wins
- `KEEP_BOTH` - Create conflict copy
- `LOCAL_WINS` - Always prefer local
- `REMOTE_WINS` - Always prefer remote
- `MANUAL` - User decides

#### **SyncMode**
- `PUSH_ONLY` - One-way backup (local → remote)
- `PULL_ONLY` - One-way import (remote → local)
- `BIDIRECTIONAL` - Full two-way sync
- `DISABLED` - No sync

### 2. NoteProvider Interface

**The heart of modularity** - any provider implementing this interface works with the sync engine.

```kotlin
interface NoteProvider {
    // CRUD
    suspend fun saveNote(note: Note, vault: Vault): Result<Unit>
    suspend fun loadNote(noteId: String, vault: Vault): Result<Note>
    suspend fun deleteNote(noteId: String, vault: Vault): Result<Unit>

    // Sync
    suspend fun listNotes(vault: Vault): Result<List<NoteMetadata>>
    suspend fun getMetadata(noteId: String, vault: Vault): Result<FileMetadata>
    suspend fun watchChanges(vault: Vault, callback: (FileEvent) -> Unit)
    suspend fun resolveConflict(
        localNote: Note,
        remoteNote: Note,
        strategy: ConflictStrategy,
        vault: Vault
    ): Result<Note>

    // Info
    suspend fun isAvailable(vault: Vault): Boolean
    fun getCapabilities(): ProviderCapabilities
}
```

### 3. FileSystemProvider (Generic File Layer)

**Used by all file-based providers** (Local, Obsidian, Logseq, etc.)

```kotlin
interface FileSystemProvider {
    suspend fun readFile(path: String): Result<String>
    suspend fun writeFile(path: String, content: String): Result<Unit>
    suspend fun deleteFile(path: String): Result<Unit>
    suspend fun listFiles(directory: String, pattern: FilePattern): Result<List<String>>
    suspend fun getMetadata(path: String): Result<FileMetadata>
    suspend fun watchDirectory(directory: String, callback: (FileEvent) -> Unit)
    suspend fun copyFile(sourcePath: String, destPath: String): Result<Unit>
    suspend fun calculateChecksum(path: String): Result<String>
}
```

**Benefits:**
- ✅ Atomic file writes (temp file + rename)
- ✅ File watching with FileObserver
- ✅ Checksum for change detection
- ✅ Path sanitization
- ✅ Reusable across all file-based providers

### 4. MarkdownParser (Generic Parsing Layer)

**Configurable markdown parsing** for different provider formats

```kotlin
interface MarkdownParser {
    fun parse(content: String, config: ParserConfig): ParsedMarkdown
    fun serialize(note: Note, config: SerializerConfig): String
    fun extractFrontmatter(content: String): Map<String, Any>
    fun extractInlineTags(content: String): List<String>
    fun extractLinks(content: String, config: ParserConfig): List<Link>
}
```

**Supports:**
- YAML frontmatter
- Inline tags (#tag)
- Wiki-style links ([[note]])
- Standard markdown links [text](url)
- Configurable per provider

## Provider Configurations

### Local Provider (Simple Folder)
```kotlin
LocalConfig(
    storagePath: String,
    fileExtension: "md",
    useSubfolders: true,
    folderStructure: FLAT | BY_DATE | BY_TAG | BY_YEAR_MONTH
)
```

### Obsidian Provider
```kotlin
ObsidianConfig(
    vaultPath: String,
    dailyNotesPath: String?,
    attachmentsPath: "attachments",
    useFrontMatter: true,
    preserveObsidianLinks: true,
    syncMode: BIDIRECTIONAL,
    conflictStrategy: LAST_WRITE_WINS,
    watchForChanges: true,
    autoSyncIntervalMinutes: 30,
    enableBacklinks: false,
    enableTemplateVariables: true
)
```

### Notion Provider
```kotlin
NotionConfig(
    workspaceId: String,
    databaseId: String?,
    apiKey: String?,
    syncMode: BIDIRECTIONAL,
    defaultPageIcon: "📝",
    mapTagsToMultiSelect: true
)
```

### Capacities Provider
```kotlin
CapacitiesConfig(
    workspaceId: String,
    apiKey: String?,
    spaceId: String?,
    syncMode: BIDIRECTIONAL
)
```

## Sync Flow

```
1. Note captured locally
   ↓
2. Saved to Room database (isSynced = false)
   ↓
3. SyncCoordinator detects pending upload
   ↓
4. Calls NoteProvider.saveNote()
   ↓
5. Provider-specific implementation:
   - LocalProvider → writes to local folder
   - ObsidianProvider → writes to vault folder
   - NotionProvider → calls Notion API
   ↓
6. Update SyncState (status = SYNCED)
   ↓
7. FileSystemProvider watches for external changes
   ↓
8. On external change → trigger pull sync
   ↓
9. Conflict detection (compare timestamps)
   ↓
10. Resolve using ConflictStrategy
```

## Key Design Principles

### 1. **Provider-Agnostic Sync Engine**
- SyncCoordinator doesn't know about Obsidian, Notion, or any specific provider
- Works purely through NoteProvider interface
- Add new provider = implement interface

### 2. **Layered Abstraction**
- **Domain Layer**: Sync logic, conflict resolution, state management
- **Data Layer**: Provider implementations, file operations, parsing
- **No cross-contamination**: Obsidian-specific code never leaks into sync engine

### 3. **Composition Over Inheritance**
- FileSystemProvider is used by providers, not inherited
- MarkdownParser is injected, not hardcoded
- Strategies are pluggable

### 4. **Local-First**
- All notes captured locally first (Room database)
- Sync happens in background
- Works offline seamlessly

### 5. **Reactive Updates**
- Room Flows for database changes
- FileObserver for file system changes
- SyncState updates trigger UI updates

## Adding a New Provider

To add support for a new note-taking platform:

### 1. Define Configuration
```kotlin
data class NewProviderConfig(
    val apiKey: String,
    val workspaceId: String,
    val syncMode: SyncMode = SyncMode.BIDIRECTIONAL,
    val conflictStrategy: ConflictStrategy = ConflictStrategy.LAST_WRITE_WINS
) : ProviderConfig()
```

### 2. Implement NoteProvider
```kotlin
class NewProvider @Inject constructor(
    // Inject dependencies
) : NoteProvider {
    override suspend fun saveNote(note: Note, vault: Vault): Result<Unit> {
        // Implementation specific to new provider
    }

    // Implement other methods...
}
```

### 3. Register in DI
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {
    @Binds
    @Named("new_provider")
    abstract fun bindNewProvider(impl: NewProvider): NoteProvider
}
```

### 4. Done!
The sync engine automatically works with the new provider through the interface.

## Database Schema

### Notes Table (Existing)
- Local storage for all notes
- `isSynced` field tracks sync status

### SyncStates Table (New)
```sql
CREATE TABLE sync_states (
    noteId TEXT PRIMARY KEY,
    vaultId TEXT NOT NULL,
    status TEXT NOT NULL,
    localModifiedAt INTEGER NOT NULL,
    remoteModifiedAt INTEGER,
    lastSyncedAt INTEGER,
    remotePath TEXT,
    retryCount INTEGER NOT NULL,
    errorMessage TEXT
)
```

## Testing Strategy

### Unit Tests
- ✅ Domain models with business logic
- ✅ Sync state calculations (hasConflict, needsPush, needsPull)
- ✅ Parser (frontmatter, tags, links)
- ✅ Conflict resolution strategies

### Integration Tests
- ✅ FileSystemProvider with temp directories
- ✅ Providers with test vaults
- ✅ Sync coordinator with mock providers
- ✅ Database operations

### End-to-End Tests
- ✅ Complete sync flow
- ✅ Widget → Note → Vault → Provider
- ✅ External changes → Pull sync
- ✅ Conflict scenarios

## Current Implementation Status

### ✅ Completed
- [x] SyncState domain model with helper methods
- [x] FileMetadata and FileEvent models
- [x] Extended ProviderConfig for all providers
- [x] SyncStateEntity and DAO with comprehensive queries
- [x] Extended NoteProvider interface with sync methods
- [x] FileSystemProvider interface
- [x] AndroidFileSystemProvider implementation
- [x] MarkdownParser interface
- [x] Enhanced ProviderCapabilities

### 🚧 In Progress
- [ ] MarkdownParser implementation
- [ ] SyncStateRepository
- [ ] SyncCoordinator implementation
- [ ] Enhanced ObsidianProvider
- [ ] Database migration

### 📋 Planned
- [ ] LocalProvider implementation
- [ ] Background sync worker
- [ ] Conflict resolution UI
- [ ] Vault settings UI
- [ ] NotionProvider (future)
- [ ] CapacitiesProvider (future)

## Benefits of This Architecture

### For Developers
- ✅ **Easy to add providers**: Just implement NoteProvider interface
- ✅ **Testable**: Every layer can be mocked
- ✅ **Maintainable**: Clear separation of concerns
- ✅ **Reusable**: FileSystemProvider and MarkdownParser shared across providers

### For Users
- ✅ **Flexible**: Choose any provider or multiple vaults
- ✅ **Reliable**: Offline-first with conflict resolution
- ✅ **Fast**: Local-first means instant capture
- ✅ **Safe**: Atomic file operations, no data loss

### For Future
- ✅ **Extensible**: New providers without touching sync engine
- ✅ **Scalable**: Modular design supports growth
- ✅ **Adaptable**: Different providers can have different features
- ✅ **Future-proof**: Architecture designed for evolution

---

**Next Steps**: Continue with SyncStateRepository implementation and SyncCoordinator core logic.
