# NoteDrop - Implementation Tasks for AI Agents

**Project**: NoteDrop Android - Quick Note Capture with Obsidian Integration
**Last Updated**: 2025-11-25
**Status**: Active Development

## Table of Contents
1. [Project Overview](#project-overview)
2. [Current Implementation Status](#current-implementation-status)
3. [High Priority Tasks](#high-priority-tasks)
4. [Medium Priority Tasks](#medium-priority-tasks)
5. [Low Priority Tasks](#low-priority-tasks)
6. [Testing Tasks](#testing-tasks)
7. [Documentation Tasks](#documentation-tasks)
8. [Technical Debt & Improvements](#technical-debt--improvements)

---

## Project Overview

NoteDrop is an Android note-capture application with platform-agnostic storage supporting Obsidian vaults. The app enables quick capture of text, voice, and photos which are seamlessly saved to Obsidian daily notes or inbox files.

### Core Architecture
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt
- **Database**: Room
- **Background Work**: WorkManager + Hilt
- **Storage**: Storage Access Framework (SAF) + DocumentFile API
- **Testing**: JUnit 4, MockK, Robolectric, Truth

### Key Features Implemented
✅ Quick capture UI (Text, Voice, Camera)
✅ Obsidian provider with daily notes support
✅ Format string parser (Moment.js tokens)
✅ Room database with sync state tracking
✅ Background sync with retry queue
✅ Home screen with note filtering
✅ Settings screen with vault management
✅ Widget support (4 widget types)
✅ Voice recording with foreground service
✅ Camera capture with CameraX

---

## Current Implementation Status

### ✅ Fully Implemented
- [x] Data layer: Room entities, DAOs, database setup
- [x] Domain layer: Models, repositories
- [x] Provider layer: ObsidianProvider, LocalProvider
- [x] UI layer: HomeScreen, SettingsScreen, QuickCaptureScreen
- [x] Sync: SyncCoordinator, SyncWorker, SyncQueue
- [x] Widgets: 4 widget types with interactive actions
- [x] Obsidian format parser with literal text support (fixed bug)
- [x] Hilt WorkManager integration (fixed initialization)
- [x] Database schema v4 with migrations removed

### 🚧 Partially Implemented
- [ ] ObsidianProvider TODO methods (loadNote, deleteNote, getMetadata)
- [ ] Voice recording widget implementation
- [ ] Camera capture widget implementation
- [ ] Comprehensive test coverage
- [ ] Error handling edge cases
- [ ] Settings validation & error messages

### ❌ Not Implemented
- [ ] Simple folder provider (SimpleFolderNoteSaver from spec)
- [ ] Batch note operations
- [ ] Conflict resolution for concurrent edits
- [ ] Local backup/cache before save
- [ ] Template support for captures
- [ ] Tag extraction and formatting
- [ ] Search functionality
- [ ] Export features
- [ ] Performance monitoring
- [ ] Analytics/crash reporting

---

## High Priority Tasks

### HP-1: Complete ObsidianProvider TODO Methods
**Priority**: Critical
**Complexity**: Medium
**Estimated Time**: 4-6 hours
**Dependencies**: None

**Description**: Implement the 3 TODO methods in ObsidianProvider that are currently stubbed.

**Tasks**:
1. **loadNote(noteId, vault)**
   - Parse noteId to get file path
   - Use DocumentFile to find file
   - Read markdown content
   - Parse front-matter if present
   - Return Note domain model

2. **deleteNote(noteId, vault)**
   - Find note file using noteId
   - Delete file using DocumentFile.delete()
   - Handle errors (permission denied, file not found)
   - Return Result<Unit>

3. **getMetadata(noteId, vault)**
   - Read file metadata (size, lastModified)
   - Parse front-matter for custom metadata
   - Return NoteMetadata object

**Files to Modify**:
- `app/src/main/java/app/notedrop/android/data/provider/ObsidianProvider.kt`

**Test Requirements**:
- Unit tests for each method
- Integration tests with mock vault structure
- Error case testing (permissions, missing files)

**Acceptance Criteria**:
- [ ] All 3 methods fully implemented
- [ ] Error handling for all edge cases
- [ ] Unit tests with >80% coverage
- [ ] Integration tests pass
- [ ] No compilation warnings

---

### HP-2: Implement SimpleFolderNoteSaver
**Priority**: High
**Complexity**: Low
**Estimated Time**: 3-4 hours
**Dependencies**: None

**Description**: Implement the SimpleFolderNoteSaver from the specification to support non-Obsidian folder storage.

**Tasks**:
1. Create `SimpleFolderNoteSaver` class implementing `NoteSaver` interface
2. Implement `saveNote()` for INBOX_FILE mode only
3. Implement `validateConfiguration()`
4. Implement `getStorageInfo()`
5. Add file naming strategies (TIMESTAMP, TITLE_BASED, UUID)
6. Add proper error handling

**Implementation Details**:
```kotlin
class SimpleFolderNoteSaver(
    private val context: Context
) : NoteSaver {
    override suspend fun saveNote(note: CapturedNote, settings: StorageSettings): SaveResult
    override suspend fun validateConfiguration(settings: StorageSettings): Result<Boolean>
    override suspend fun getStorageInfo(settings: StorageSettings): StorageInfo?
}
```

**Files to Create**:
- `app/src/main/java/app/notedrop/android/data/provider/SimpleFolderNoteSaver.kt`

**Files to Modify**:
- `app/src/main/java/app/notedrop/android/di/RepositoryModule.kt` (add provider)
- `app/src/main/java/app/notedrop/android/ui/settings/SettingsScreen.kt` (add simple folder option)

**Test Requirements**:
- Unit tests for all methods
- SAF integration tests
- File naming strategy tests

**Acceptance Criteria**:
- [ ] SimpleFolderNoteSaver fully implemented
- [ ] All error cases handled
- [ ] Settings UI supports simple folder selection
- [ ] Tests pass with >80% coverage
- [ ] Daily notes gracefully disabled for simple folders

---

### HP-3: Fix Database Migration & Clean App Data
**Priority**: Critical
**Complexity**: Low
**Estimated Time**: 1 hour
**Dependencies**: None

**Description**: The migrations were removed to rebuild database from scratch. Need to ensure clean deployment.

**Tasks**:
1. Verify database builds correctly from schema v4
2. Add database seeding for first-time users
3. Create migration guide for existing users
4. Test database initialization on fresh install
5. Document database schema changes

**Files to Verify**:
- `app/src/main/java/app/notedrop/android/data/local/NoteDropDatabase.kt`
- `app/src/main/java/app/notedrop/android/di/DatabaseModule.kt`
- All entity files

**Acceptance Criteria**:
- [ ] Fresh install creates database v4 correctly
- [ ] All indices are created properly
- [ ] No migration errors on clean install
- [ ] Database Inspector shows correct schema
- [ ] Foreign keys validated

---

### HP-4: Voice Recording Widget Complete Implementation
**Priority**: High
**Complexity**: Medium
**Estimated Time**: 6-8 hours
**Dependencies**: VoiceRecordingService exists

**Description**: Complete the voice recording widget with full recording/playback functionality.

**Tasks**:
1. Implement widget UI with recording indicator
2. Connect to VoiceRecordingService
3. Handle service lifecycle (start/stop)
4. Add recording progress feedback
5. Save recording and attach to note
6. Handle errors (permissions, storage full)
7. Add playback controls
8. Widget configuration screen

**Files to Modify/Create**:
- `app/src/main/java/app/notedrop/android/ui/widget/VoiceCaptureWidget.kt` (new)
- `app/src/main/java/app/notedrop/android/ui/widget/service/VoiceRecordingService.kt` (modify)
- Widget XML layouts

**Acceptance Criteria**:
- [ ] Widget shows recording state
- [ ] Recording starts/stops correctly
- [ ] Audio file saved to correct location
- [ ] Note created with voice attachment
- [ ] Error handling for all edge cases
- [ ] Permissions requested appropriately
- [ ] Widget updates reflect recording state

---

### HP-5: Camera Capture Widget Complete Implementation
**Priority**: High
**Complexity**: High
**Estimated Time**: 8-10 hours
**Dependencies**: TransparentCameraActivity exists

**Description**: Complete the camera capture widget with instant photo capture.

**Tasks**:
1. Implement widget UI
2. Launch TransparentCameraActivity on tap
3. Capture photo instantly
4. Save photo to appropriate location
5. Create note with photo attachment
6. Handle errors (permissions, storage)
7. Image compression/optimization
8. Widget configuration

**Files to Modify/Create**:
- `app/src/main/java/app/notedrop/android/ui/widget/CameraCaptureWidget.kt` (new)
- `app/src/main/java/app/notedrop/android/ui/widget/camera/TransparentCameraActivity.kt` (modify)
- Camera utility classes

**Technical Requirements**:
- CameraX integration complete
- Proper permission handling
- Image optimization (compress to <2MB)
- Support both front/back cameras
- Handle low storage gracefully

**Acceptance Criteria**:
- [ ] Widget launches camera instantly
- [ ] Photo captured and saved
- [ ] Note created with image
- [ ] Permissions handled
- [ ] Low storage handled
- [ ] Camera initialization errors handled
- [ ] Widget reflects capture state

---

## Medium Priority Tasks

### MP-1: Comprehensive Error Handling
**Priority**: Medium
**Complexity**: Medium
**Estimated Time**: 4-6 hours

**Description**: Add comprehensive error handling throughout the app with user-friendly messages.

**Tasks**:
1. Create centralized error handler
2. Define error types and messages
3. Add error logging (Timber or similar)
4. Implement retry logic for network/storage errors
5. Add error state UI components
6. User-friendly error messages

**Areas to Cover**:
- Storage access errors
- Network errors (if applicable)
- Database errors
- Provider-specific errors
- Widget errors
- Permission errors

**Files to Create**:
- `app/src/main/java/app/notedrop/android/util/ErrorHandler.kt`
- `app/src/main/java/app/notedrop/android/util/ErrorMessages.kt`

**Acceptance Criteria**:
- [ ] All exceptions caught and handled
- [ ] User sees meaningful error messages
- [ ] Errors logged appropriately
- [ ] Retry mechanisms in place
- [ ] Error state UI implemented

---

### MP-2: Settings Validation & Feedback
**Priority**: Medium
**Complexity**: Low
**Estimated Time**: 3-4 hours

**Description**: Add comprehensive validation and feedback in settings screen.

**Tasks**:
1. Validate vault selection (check if valid Obsidian vault)
2. Test write permissions before saving
3. Show validation status in UI
4. Preview format strings before applying
5. Validate daily notes configuration
6. Show warnings for invalid configurations

**Files to Modify**:
- `app/src/main/java/app/notedrop/android/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/app/notedrop/android/ui/settings/SettingsScreen.kt`

**UI Enhancements**:
- Validation indicators (✓/✗)
- Warning messages
- Test connection button
- Preview formatted output
- Configuration hints

**Acceptance Criteria**:
- [ ] All settings validated before save
- [ ] User sees validation feedback
- [ ] Invalid configs prevented
- [ ] Test connection works
- [ ] Format preview functional

---

### MP-3: Template Support for Notes
**Priority**: Medium
**Complexity**: Medium
**Estimated Time**: 5-7 hours

**Description**: Implement template support for captured notes as specified in the original spec.

**Tasks**:
1. Create Template entity and DAO
2. Template CRUD operations
3. Template selection UI
4. Apply template to new notes
5. Support template variables ({{date}}, {{time}}, etc.)
6. Template preview

**Database Changes**:
- Template table already exists (TemplateEntity)
- Need TemplateDao implementation
- Template repository

**Files to Modify/Create**:
- `app/src/main/java/app/notedrop/android/domain/model/Template.kt`
- `app/src/main/java/app/notedrop/android/data/local/dao/TemplateDao.kt` (complete)
- `app/src/main/java/app/notedrop/android/ui/template/TemplateScreen.kt` (new)
- Template manager

**Acceptance Criteria**:
- [ ] Templates can be created/edited/deleted
- [ ] Template variables resolved correctly
- [ ] UI for template selection
- [ ] Templates applied to captured notes
- [ ] Template preview works

---

### MP-4: Batch Note Operations
**Priority**: Medium
**Complexity**: Medium
**Estimated Time**: 4-6 hours

**Description**: Implement batch operations for managing multiple notes at once.

**Tasks**:
1. Multi-select mode in HomeScreen
2. Batch delete notes
3. Batch move to different vault
4. Batch tag editing
5. Batch export
6. Progress indicators for batch operations

**Files to Modify**:
- `app/src/main/java/app/notedrop/android/ui/home/HomeScreen.kt`
- `app/src/main/java/app/notedrop/android/ui/home/HomeViewModel.kt`
- Repository layer for batch operations

**UI Requirements**:
- Selection mode toggle
- Select all/none buttons
- Batch action menu
- Progress dialog
- Undo support

**Acceptance Criteria**:
- [ ] Multi-select works smoothly
- [ ] Batch delete implemented
- [ ] Batch move implemented
- [ ] Progress feedback shown
- [ ] Undo option available

---

### MP-5: Search Functionality
**Priority**: Medium
**Complexity**: Medium
**Estimated Time**: 5-7 hours

**Description**: Implement full-text search across notes with filters.

**Tasks**:
1. Enhance search query in Room
2. Full-text search implementation
3. Search filters (date, vault, tags)
4. Search suggestions/history
5. Highlight search terms in results
6. Search performance optimization

**Database Changes**:
- Add FTS (Full-Text Search) table
- Create search indices
- Optimize queries

**Files to Create/Modify**:
- `app/src/main/java/app/notedrop/android/data/local/dao/SearchDao.kt`
- `app/src/main/java/app/notedrop/android/ui/search/SearchScreen.kt`
- `app/src/main/java/app/notedrop/android/ui/search/SearchViewModel.kt`

**Acceptance Criteria**:
- [ ] Full-text search works
- [ ] Filters functional
- [ ] Search fast (<500ms)
- [ ] Results highlighted
- [ ] Search history saved

---

## Low Priority Tasks

### LP-1: Conflict Resolution for Concurrent Edits
**Priority**: Low
**Complexity**: High
**Estimated Time**: 10-12 hours

**Description**: Handle conflicts when the same daily note is edited concurrently.

**Tasks**:
1. Detect concurrent modifications
2. Implement conflict resolution strategies
3. Three-way merge algorithm
4. Conflict UI for user resolution
5. Automatic conflict resolution options

**Implementation Approach**:
- Track file hashes/timestamps
- Compare local vs remote state
- Offer merge strategies (ours, theirs, manual)

---

### LP-2: Local Backup/Cache System
**Priority**: Low
**Complexity**: Medium
**Estimated Time**: 6-8 hours

**Description**: Implement local caching of notes before sync with backup recovery.

**Tasks**:
1. Cache notes locally before saving
2. Retry failed saves from cache
3. Backup recovery UI
4. Automatic cleanup of old backups
5. Export backup data

---

### LP-3: Tag Extraction & Auto-formatting
**Priority**: Low
**Complexity**: Low
**Estimated Time**: 3-4 hours

**Description**: Automatically detect and format hashtags in captured notes.

**Tasks**:
1. Regex pattern for hashtag detection
2. Auto-formatting in preview
3. Tag suggestions
4. Tag autocomplete
5. Tag cloud visualization

---

### LP-4: Export Features
**Priority**: Low
**Complexity**: Medium
**Estimated Time**: 5-7 hours

**Description**: Export notes to various formats.

**Tasks**:
1. Export to markdown
2. Export to PDF
3. Export to HTML
4. Bulk export
5. Export configuration

---

### LP-5: Performance Monitoring & Analytics
**Priority**: Low
**Complexity**: Medium
**Estimated Time**: 4-6 hours

**Description**: Add performance monitoring and crash reporting.

**Tasks**:
1. Integrate Firebase Analytics (optional)
2. Performance monitoring for critical paths
3. Crash reporting
4. Usage analytics
5. Performance dashboard

---

## Testing Tasks

### T-1: Unit Test Coverage to >80%
**Priority**: High
**Complexity**: Medium
**Estimated Time**: 12-16 hours

**Current Coverage**: ~30% estimated
**Target Coverage**: >80%

**Areas Needing Tests**:
1. **ObsidianProvider** (current: ~10%)
   - Format parser tests (exist, need expansion)
   - loadNote/deleteNote/getMetadata tests
   - Daily notes tests (exist)
   - Error handling tests

2. **Repositories** (current: ~20%)
   - CRUD operations
   - Flow transformations
   - Error cases

3. **ViewModels** (current: ~5%)
   - State management
   - User actions
   - Flow collection

4. **SyncCoordinator** (current: ~0%)
   - Sync logic
   - Retry mechanism
   - Error handling

5. **Utilities** (current: ~40%)
   - Date formatters
   - String utilities
   - File operations

**Test Files to Create/Expand**:
- `ObsidianProviderTest.kt` (expand)
- `LocalProviderTest.kt` (create)
- `NoteRepositoryImplTest.kt` (create)
- `VaultRepositoryImplTest.kt` (create)
- `SyncCoordinatorTest.kt` (create)
- `HomeViewModelTest.kt` (create)
- `SettingsViewModelTest.kt` (create)

**Testing Strategy**:
- Use MockK for mocking
- Robolectric for Android framework
- Turbine for Flow testing
- Truth for assertions

---

### T-2: Integration Tests
**Priority**: Medium
**Complexity**: High
**Estimated Time**: 10-12 hours

**Description**: End-to-end integration tests for critical flows.

**Test Scenarios**:
1. **Capture to Save Flow**
   - Capture text → Save to daily note
   - Capture voice → Save to inbox
   - Capture photo → Save with attachment

2. **Sync Flow**
   - Note capture → Background sync → Verification
   - Failed sync → Retry queue → Success

3. **Widget Flow**
   - Widget tap → Capture → Save → Widget update

**Files to Create**:
- `ProviderSyncIntegrationTest.kt` (exists, expand)
- `WidgetIntegrationTest.kt` (create)
- `EndToEndCaptureTest.kt` (create)

---

### T-3: UI Tests (Compose)
**Priority**: Low
**Complexity**: Medium
**Estimated Time**: 8-10 hours

**Description**: Compose UI tests for all screens.

**Screens to Test**:
- HomeScreen
- QuickCaptureScreen
- SettingsScreen
- Widget configuration screens

**Test Approach**:
- Use ComposeTestRule
- Semantic properties
- User interactions
- State verification

---

## Documentation Tasks

### D-1: API Documentation (KDoc)
**Priority**: Medium
**Complexity**: Low
**Estimated Time**: 4-6 hours

**Description**: Add comprehensive KDoc comments to all public APIs.

**Requirements**:
- All public classes documented
- All public methods documented
- Parameters explained
- Return values explained
- Examples where helpful

---

### D-2: Architecture Documentation
**Priority**: Medium
**Complexity**: Low
**Estimated Time**: 3-4 hours

**Description**: Document the app architecture with diagrams.

**Deliverables**:
- Architecture diagram (layers)
- Data flow diagram
- Sync process diagram
- Widget interaction diagram
- Obsidian format parsing diagram

---

### D-3: User Guide
**Priority**: Low
**Complexity**: Low
**Estimated Time**: 4-6 hours

**Description**: Create user-facing documentation.

**Contents**:
- Setup guide
- Vault configuration
- Widget setup
- Format string guide
- Troubleshooting
- FAQs

---

## Technical Debt & Improvements

### TD-1: Code Quality Improvements
**Tasks**:
- [ ] Run Lint and fix all warnings
- [ ] Run Detekt for Kotlin static analysis
- [ ] Fix all deprecation warnings
- [ ] Consistent code formatting
- [ ] Remove unused imports/code

### TD-2: Performance Optimization
**Tasks**:
- [ ] Profile database queries
- [ ] Optimize large file reads
- [ ] Reduce memory allocations
- [ ] LazyColumn performance
- [ ] Worker execution optimization

### TD-3: Accessibility Improvements
**Tasks**:
- [ ] Add content descriptions
- [ ] Keyboard navigation
- [ ] Screen reader support
- [ ] High contrast mode
- [ ] Font scaling support

### TD-4: Localization
**Tasks**:
- [ ] Extract all strings to resources
- [ ] Support RTL languages
- [ ] Date/time formatting for locales
- [ ] Plurals handling
- [ ] Translation-ready strings

---

## Task Prioritization Matrix

### Must Have (Release Blockers)
- HP-1: Complete ObsidianProvider methods
- HP-3: Fix database migration
- HP-4: Voice recording widget
- HP-5: Camera capture widget
- T-1: Unit test coverage >80%
- TD-1: Fix all critical warnings

### Should Have (Next Release)
- HP-2: SimpleFolderNoteSaver
- MP-1: Comprehensive error handling
- MP-2: Settings validation
- T-2: Integration tests
- D-1: API documentation

### Nice to Have (Future Releases)
- MP-3: Template support
- MP-4: Batch operations
- MP-5: Search functionality
- All LP tasks
- D-2: Architecture docs
- D-3: User guide

---

## Implementation Guidelines for AI Agents

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Use coroutines for async operations
- Prefer composition over inheritance

### Testing Requirements
- Write tests before implementation (TDD when possible)
- Test happy path + edge cases
- Use meaningful test names
- Aim for >80% coverage
- Use Truth assertions

### Documentation Requirements
- Add KDoc to all public APIs
- Document parameters and return values
- Add usage examples
- Explain non-obvious logic
- Keep docs up-to-date

### Git Commit Messages
- Use conventional commits format
- Include task ID in commit message
- Keep commits focused and atomic
- Write clear commit descriptions
- Reference issues/PRs when applicable

Example: `feat(HP-1): implement loadNote in ObsidianProvider`

---

## Conclusion

This document provides a comprehensive list of implementation tasks for the NoteDrop Android application. Tasks are prioritized and categorized for easy assignment to AI agents or human developers.

**Total Estimated Time**: 150-200 hours
**Critical Path**: HP tasks + core testing
**Target Completion**: Phased approach recommended

For questions or clarifications, refer to the [obsidian-integration-spec.md](../app/obsidian-integration-spec.md) specification document.

---

**Last Updated**: 2025-11-25
**Version**: 1.0
**Maintainer**: Development Team
