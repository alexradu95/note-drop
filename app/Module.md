# Module NoteDrop

NoteDrop is a privacy-first note-taking application for Android 12+ built with Material You design.

## Features

- **Quick Capture**: Capture notes in under 2 seconds
- **Voice Recording**: Record and attach voice notes
- **Obsidian Integration**: Seamless sync with Obsidian vaults
- **Home Screen Widgets**: Quick capture from your home screen
- **Material You Design**: Dynamic colors and modern UI

## Architecture

NoteDrop follows Clean Architecture principles with three main layers:

- **Presentation Layer**: Jetpack Compose UI, ViewModels, Navigation
- **Domain Layer**: Business logic, Use Cases, Repository interfaces
- **Data Layer**: Room database, File system providers, DAOs

## Key Components

### Domain Models
- `Note`: Core note entity with content, tags, and metadata
- `Vault`: Storage location for notes (Obsidian, Local, etc.)
- `Template`: Note templates for quick capture

### Repositories
- `NoteRepository`: Note CRUD operations
- `VaultRepository`: Vault management
- `SyncStateRepository`: Synchronization state tracking

### Sync Engine
- `SyncCoordinator`: Coordinates sync between local and remote storage
- `ConflictResolver`: Handles sync conflicts
