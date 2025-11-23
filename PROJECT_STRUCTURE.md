# NoteDrop - Project Structure

## Overview
NoteDrop is a privacy-first, multi-platform note capture app built for Android 12+ (API 31).

## Technology Stack

### Core Technologies
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern declarative UI
- **Material 3**: Material You dynamic theming
- **Hilt**: Dependency injection
- **Room**: Local database
- **Coroutines + Flow**: Asynchronous programming
- **Navigation Compose**: Navigation framework

### API Level
- **Minimum SDK**: 31 (Android 12)
- **Target SDK**: 35 (Android 14)
- **Compile SDK**: 35

## Architecture

The project follows **Clean Architecture** with three main layers:

### 1. Domain Layer (`domain/`)
Contains business logic and domain models.

```
domain/
├── model/
│   ├── Note.kt          # Core note model
│   ├── Vault.kt         # Vault and provider configuration
│   └── Template.kt      # Note templates
├── repository/          # Repository interfaces (to be created)
└── usecase/             # Business logic use cases (to be created)
```

### 2. Data Layer (`data/`)
Handles data sources and implementations.

```
data/
├── local/
│   ├── entity/
│   │   ├── NoteEntity.kt      # Room entity for notes
│   │   ├── VaultEntity.kt     # Room entity for vaults
│   │   └── TemplateEntity.kt  # Room entity for templates
│   ├── dao/
│   │   ├── NoteDao.kt         # Data access for notes
│   │   ├── VaultDao.kt        # Data access for vaults
│   │   └── TemplateDao.kt     # Data access for templates
│   └── NoteDropDatabase.kt    # Main database class
├── provider/            # Provider implementations (to be created)
└── repository/          # Repository implementations (to be created)
```

### 3. Presentation Layer (`ui/`)
UI components and ViewModels.

```
ui/
├── theme/
│   ├── Theme.kt         # Material You theming
│   ├── Color.kt         # Color definitions
│   └── Type.kt          # Typography definitions
├── capture/             # Capture screens (to be created)
├── notes/               # Note list/detail screens (to be created)
├── settings/            # Settings screens (to be created)
└── widgets/             # Home screen widgets (to be created)
```

### 4. Dependency Injection (`di/`)
Hilt modules for dependency injection.

```
di/
└── DatabaseModule.kt    # Database dependencies
```

## Key Features Implemented

### ✅ Completed Setup
1. **Build Configuration**
   - All dependencies configured in `libs.versions.toml`
   - Hilt and KSP plugins applied
   - Room database setup

2. **Database Schema**
   - Note entity with full metadata support
   - Vault entity with provider configurations
   - Template entity with variable support
   - Complete DAO operations for all entities

3. **Dependency Injection**
   - Hilt application setup
   - Database module providing DAOs
   - MainActivity injection ready

4. **Material You Theming**
   - Dynamic color scheme from wallpaper
   - Dark/Light mode support
   - Edge-to-edge experience

5. **Splash Screen (Android 12+)**
   - Professional app launch
   - Smooth transition to main app
   - Material You colors

## Domain Models

### Note
```kotlin
data class Note(
    val id: String,
    val content: String,
    val title: String?,
    val vaultId: String,
    val tags: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val voiceRecordingPath: String?,
    val transcriptionStatus: TranscriptionStatus,
    val metadata: Map<String, String>,
    val isSynced: Boolean
)
```

### Vault
```kotlin
data class Vault(
    val id: String,
    val name: String,
    val description: String?,
    val providerType: ProviderType,
    val providerConfig: ProviderConfig,
    val isDefault: Boolean,
    val isEncrypted: Boolean,
    val createdAt: Instant,
    val lastSyncedAt: Instant?
)
```

### Template
```kotlin
data class Template(
    val id: String,
    val name: String,
    val content: String,
    val description: String?,
    val variables: List<String>,
    val isBuiltIn: Boolean,
    val createdAt: Instant,
    val usageCount: Int
)
```

## Provider Types

- **LOCAL**: Local device storage
- **OBSIDIAN**: Obsidian vault integration
- **NOTION**: Notion workspace (future)
- **CUSTOM**: Custom provider extensibility

## Built-in Templates

1. **Quick Capture**: Simple note with just content
2. **Daily Note**: Daily note with date header
3. **Meeting Note**: Meeting notes with action items

## Next Steps

### High Priority
1. **Repository Layer**: Implement repository pattern
2. **Use Cases**: Create business logic use cases
3. **ViewModels**: Create ViewModels for screens
4. **UI Screens**:
   - Quick Capture screen
   - Note List screen
   - Note Detail screen
   - Settings screen
5. **Obsidian Provider**: File-based vault integration

### Medium Priority
1. **Voice Recording**: Audio recording functionality
2. **Transcription**: Basic transcription service
3. **Widgets**: Home screen quick capture widget
4. **Search**: Full-text search implementation
5. **Tags**: Tag management UI

### Future Features
1. **Advanced Transcription**: Whisper model integration
2. **Multi-device Sync**: P2P synchronization
3. **Wear OS**: Companion app
4. **Android TV**: Family hub features
5. **Android Auto**: Voice-first capture

## Development Commands

### Build
```bash
./gradlew build
```

### Run
```bash
./gradlew installDebug
```

### Test
```bash
./gradlew test
```

### Generate Database Schema
```bash
./gradlew :app:kspDebugKotlin
# Schemas will be in: app/schemas/
```

## Privacy & Security

- **Local-First**: All data stored locally by default
- **Optional Encryption**: E2E encryption for sensitive notes
- **No Analytics**: No tracking without explicit consent
- **Zero Knowledge**: Server (if used) cannot read notes
- **GDPR Compliant**: Full data export/delete capabilities

## Performance Targets

- App launch: < 1 second
- Capture time: < 500ms
- Widget update: < 100ms
- Battery impact: < 2% per hour
- Memory usage: < 100MB

## Version

- **Current Version**: 1.0.0
- **Version Code**: 1
- **Database Version**: 1

---

**Last Updated**: 2025-11-23
