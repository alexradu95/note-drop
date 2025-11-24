# NoteDrop Development Roadmap 🗺️

## Overview

This document outlines all remaining features, improvements, and tasks for NoteDrop.

---

## ✅ **Completed (MVP)**

### Core Architecture
- ✅ Clean Architecture setup (Domain, Data, Presentation)
- ✅ Hilt dependency injection
- ✅ Room database with 3 entities
- ✅ Repository pattern (3 repositories)
- ✅ Kotlin Coroutines + Flow

### Database Layer
- ✅ Note entity with voice, tags, metadata
- ✅ Vault entity with provider configs
- ✅ Template entity with built-in templates
- ✅ Complete DAOs (15+ operations each)

### Domain Layer
- ✅ Domain models (Note, Vault, Template)
- ✅ Repository interfaces
- ✅ TranscriptionStatus enum
- ✅ ProviderType enum
- ✅ ProviderConfig sealed class

### Provider System
- ✅ Provider interface
- ✅ Obsidian provider with Markdown formatting
- ✅ Front-matter support
- ✅ Daily notes path support
- ✅ File name generation

### Voice Features
- ✅ Voice recording (VoiceRecorder)
- ✅ Voice playback (VoicePlayer)
- ✅ Recording state management
- ✅ Pause/Resume (Android 24+)
- ✅ File management

### User Interface
- ✅ Material You dynamic theming
- ✅ Splash screen (Android 12+)
- ✅ Quick Capture screen
- ✅ Home screen with notes list
- ✅ Settings screen with vault management
- ✅ Navigation system
- ✅ Search functionality
- ✅ 4 filter modes (All, Today, Voice, Tagged)

### ViewModels
- ✅ QuickCaptureViewModel
- ✅ HomeViewModel
- ✅ SettingsViewModel

### Testing
- ✅ Test infrastructure setup
- ✅ Test utilities (fixtures, fakes, dispatchers)
- ✅ Domain layer tests (39 tests)
- ✅ Repository tests (16 tests)
- ✅ ViewModel tests (14 tests)
- ✅ Testing documentation

### Documentation
- ✅ README.md
- ✅ PROJECT_STRUCTURE.md
- ✅ IMPLEMENTATION_SUMMARY.md
- ✅ TESTING_GUIDE.md
- ✅ TEST_IMPLEMENTATION_SUMMARY.md

---

## 🚧 **In Progress / High Priority**

### 1. Testing Completion ⚡ **HIGH PRIORITY**

**Remaining: ~150 tests**

#### Unit Tests (test/)
- [ ] **VaultRepositoryImplTest** (~10 tests)
  - CRUD operations
  - Default vault logic
  - Last synced updates

- [ ] **TemplateRepositoryImplTest** (~10 tests)
  - CRUD operations
  - Built-in template initialization
  - Usage count increment

- [ ] **QuickCaptureViewModelTest** (~15 tests)
  - Content/title changes
  - Template processing
  - Tag management
  - Voice recording integration
  - Save flow with errors

- [ ] **SettingsViewModelTest** (~10 tests)
  - Vault CRUD operations
  - Default vault setting
  - Error handling

- [ ] **ObsidianProviderTest** (~12 tests)
  - Markdown formatting
  - Front-matter generation
  - File path generation
  - Template variable processing

- [ ] **VoiceRecorderTest** (~8 tests)
  - State transitions
  - File management
  - Error handling

- [ ] **VoicePlayerTest** (~7 tests)
  - Playback states
  - Seek functionality

#### Instrumented Tests (androidTest/)
- [ ] **NoteDaoTest** (~15 tests)
  - In-memory database
  - All DAO operations
  - Flow emissions
  - Search queries

- [ ] **VaultDaoTest** (~12 tests)
  - Default vault logic
  - CRUD operations

- [ ] **TemplateDaoTest** (~10 tests)
  - Usage count
  - Built-in vs user templates

- [ ] **QuickCaptureScreenTest** (~15 tests)
  - UI rendering
  - User interactions
  - Navigation

- [ ] **HomeScreenTest** (~12 tests)
  - Note list display
  - Search interaction
  - Filter selection

- [ ] **SettingsScreenTest** (~10 tests)
  - Vault management UI
  - Form validation
  - Dialog interactions

- [ ] **ObsidianProviderIntegrationTest** (~8 tests)
  - Real file I/O
  - File creation verification

- [ ] **NoteCreationFlowTest** (~5 tests)
  - End-to-end note creation

- [ ] **MultiVaultSyncTest** (~5 tests)
  - Multiple vault operations

**Estimated Time:** 6-8 hours

---

### 2. Runtime Permissions 🔐 **HIGH PRIORITY**

Currently permissions are declared in manifest but not requested at runtime.

**Tasks:**
- [ ] Create permission request composable
- [ ] Request RECORD_AUDIO permission before recording
- [ ] Request storage permissions for Obsidian vault access
- [ ] Handle permission denial gracefully
- [ ] Show rationale dialogs
- [ ] Settings deep link for denied permissions

**Files to Create:**
- `ui/permissions/PermissionHandler.kt`
- `ui/permissions/PermissionDialog.kt`

**Estimated Time:** 2-3 hours

---

### 3. Error Handling & User Feedback 🚨 **HIGH PRIORITY**

Current error handling exists but needs better UI feedback.

**Tasks:**
- [ ] Global error handling with SnackBar
- [ ] Loading states for all async operations
- [ ] Better error messages (user-friendly)
- [ ] Retry mechanisms
- [ ] Network error handling (for future cloud features)
- [ ] Vault availability checking with user feedback

**Files to Update:**
- All ViewModel files
- All Screen files

**Estimated Time:** 3-4 hours

---

## 📱 **Essential Features**

### 4. Note Editing 📝 **MEDIUM PRIORITY**

Currently can only create notes, not edit them.

**Tasks:**
- [ ] Note detail/edit screen
- [ ] Edit existing notes
- [ ] Update timestamp on edit
- [ ] Re-sync to Obsidian on edit
- [ ] Delete note functionality
- [ ] Navigation to edit screen from home

**Files to Create:**
- `ui/detail/NoteDetailScreen.kt`
- `ui/detail/NoteDetailViewModel.kt`
- Update `navigation/NoteDropNavigation.kt`

**Estimated Time:** 4-5 hours

---

### 5. Home Screen Widget (Glance) 📲 **MEDIUM PRIORITY**

Quick capture from home screen.

**Tasks:**
- [ ] Create Glance widget for quick capture
- [ ] Widget configuration screen
- [ ] Multiple widget sizes (small, medium, large)
- [ ] Material You widget theming
- [ ] Widget preview images
- [ ] Widget update receiver

**Files to Create:**
- `ui/widget/QuickCaptureWidget.kt`
- `ui/widget/QuickCaptureWidgetReceiver.kt`
- Widget layout resources
- Widget configuration activity

**Estimated Time:** 5-6 hours

---

### 6. Voice Transcription 🎤 **MEDIUM PRIORITY**

Currently only records, doesn't transcribe.

**Tasks:**
- [ ] Integrate Whisper model or Speech Recognition API
- [ ] Background transcription service
- [ ] Transcription status updates
- [ ] Edit transcription option
- [ ] Transcription language selection
- [ ] Progress indicator during transcription

**Options:**
1. **Android Speech Recognition API** (Quick, cloud-based)
2. **Whisper Model** (Privacy-first, on-device, larger app size)
3. **Both** (User choice)

**Files to Create:**
- `data/voice/VoiceTranscriber.kt`
- `data/voice/WhisperModel.kt` (if using Whisper)
- `ui/transcription/TranscriptionScreen.kt`

**Estimated Time:** 8-10 hours (depending on approach)

---

### 7. Export/Import 📦 **LOW PRIORITY**

Data portability and backup.

**Tasks:**
- [ ] Export all notes as ZIP (Markdown files)
- [ ] Export single vault
- [ ] Import from ZIP
- [ ] GDPR compliance (export all data)
- [ ] JSON export option
- [ ] CSV export option
- [ ] Share notes functionality

**Files to Create:**
- `data/export/ExportManager.kt`
- `data/export/ImportManager.kt`
- `ui/export/ExportScreen.kt`

**Estimated Time:** 6-8 hours

---

### 8. Advanced Search 🔍 **LOW PRIORITY**

Better search capabilities.

**Tasks:**
- [ ] Full-text search with Room FTS
- [ ] Search by date range
- [ ] Search by vault
- [ ] Search history
- [ ] Search suggestions
- [ ] Advanced filters UI
- [ ] Saved searches

**Files to Update:**
- `data/local/dao/NoteDao.kt` (add FTS table)
- `ui/home/HomeScreen.kt` (advanced search UI)
- `ui/home/HomeViewModel.kt`

**Estimated Time:** 4-5 hours

---

## 🎨 **Polish & UX Improvements**

### 9. UI Polish ✨ **ONGOING**

**Tasks:**
- [ ] Add animations and transitions
- [ ] Improve empty states
- [ ] Better loading indicators
- [ ] Swipe to delete notes
- [ ] Pull to refresh
- [ ] Note preview on long press
- [ ] Haptic feedback
- [ ] Accessibility improvements (TalkBack, large text)
- [ ] Landscape mode optimization
- [ ] Tablet UI optimization

**Estimated Time:** 6-8 hours

---

### 10. Onboarding & Help 📚 **LOW PRIORITY**

**Tasks:**
- [ ] First-launch onboarding flow
- [ ] Tutorial for key features
- [ ] Help screen
- [ ] FAQ section
- [ ] Video tutorials (external)
- [ ] Quick tips

**Files to Create:**
- `ui/onboarding/OnboardingScreen.kt`
- `ui/help/HelpScreen.kt`

**Estimated Time:** 4-5 hours

---

## 🔧 **Technical Improvements**

### 11. Performance Optimization ⚡ **LOW PRIORITY**

**Tasks:**
- [ ] Database indexing for faster queries
- [ ] Pagination for note list
- [ ] Image loading optimization (when images added)
- [ ] Background sync worker
- [ ] App startup time optimization
- [ ] Memory leak detection
- [ ] ProGuard/R8 optimization

**Estimated Time:** 4-6 hours

---

### 12. Observability & Debugging 🐛 **LOW PRIORITY**

**Tasks:**
- [ ] Timber logging setup
- [ ] Crash reporting (Firebase Crashlytics or similar)
- [ ] Analytics (privacy-friendly, opt-in)
- [ ] Performance monitoring
- [ ] Debug menu for development

**Estimated Time:** 3-4 hours

---

### 13. Provider Enhancements 🔌 **LOW PRIORITY**

**Tasks:**
- [ ] Notion provider implementation
- [ ] Custom provider SDK
- [ ] Provider plugin system
- [ ] Sync conflict resolution
- [ ] Bi-directional sync (import from providers)
- [ ] Batch sync operations
- [ ] Sync status indicators

**Files to Create:**
- `data/provider/NotionProvider.kt`
- `data/provider/ProviderSdk.kt`

**Estimated Time:** 10-15 hours

---

## 🚀 **Advanced Features**

### 14. Rich Text Editor 📄 **FUTURE**

**Tasks:**
- [ ] Markdown editor with preview
- [ ] Rich text formatting toolbar
- [ ] Code block syntax highlighting
- [ ] Link detection and preview
- [ ] Checkbox support for tasks
- [ ] Table support
- [ ] Image embedding

**Estimated Time:** 12-15 hours

---

### 15. Image Attachments 🖼️ **FUTURE**

**Tasks:**
- [ ] Camera integration
- [ ] Gallery picker
- [ ] Image compression
- [ ] Image storage in vault
- [ ] Image preview in notes
- [ ] OCR for image text extraction

**Estimated Time:** 8-10 hours

---

### 16. Note Linking & Graph 🔗 **FUTURE**

**Tasks:**
- [ ] Wiki-style note linking [[Note]]
- [ ] Backlinks display
- [ ] Graph view of note connections
- [ ] Tag hierarchy
- [ ] Note templates with links

**Estimated Time:** 15-20 hours

---

### 17. Collaboration Features 🤝 **FUTURE**

**Tasks:**
- [ ] Shared vaults
- [ ] Real-time sync
- [ ] Conflict resolution UI
- [ ] User mentions
- [ ] Comments on notes
- [ ] Activity feed

**Estimated Time:** 20-25 hours

---

## 🌐 **Multi-Platform**

### 18. Wear OS Companion ⌚ **FUTURE**

**Tasks:**
- [ ] Wear OS app module
- [ ] Quick capture on watch
- [ ] Voice recording on watch
- [ ] Complication provider
- [ ] Watch face integration
- [ ] Phone sync

**Estimated Time:** 15-20 hours

---

### 19. Android TV 📺 **FUTURE**

**Tasks:**
- [ ] TV app module
- [ ] Leanback UI
- [ ] Remote control navigation
- [ ] Voice search
- [ ] Family sharing
- [ ] Ambient mode

**Estimated Time:** 15-20 hours

---

### 20. Android Auto 🚗 **FUTURE**

**Tasks:**
- [ ] Auto app module
- [ ] Car App Library integration
- [ ] Voice-first interface
- [ ] Safety features
- [ ] Location context

**Estimated Time:** 10-15 hours

---

## 📊 **Priority Matrix**

### **Must Have (Next Sprint)**
1. 🔴 Runtime Permissions (2-3h)
2. 🔴 Remaining Tests (~150 tests, 6-8h)
3. 🔴 Error Handling UI (3-4h)
4. 🟡 Note Editing (4-5h)

**Total: 15-20 hours**

---

### **Should Have (Next Month)**
1. 🟡 Home Screen Widget (5-6h)
2. 🟡 Voice Transcription (8-10h)
3. 🟡 UI Polish (6-8h)
4. 🟢 Export/Import (6-8h)

**Total: 25-32 hours**

---

### **Nice to Have (Next Quarter)**
1. 🟢 Advanced Search (4-5h)
2. 🟢 Onboarding (4-5h)
3. 🟢 Rich Text Editor (12-15h)
4. 🟢 Image Attachments (8-10h)
5. 🟢 Performance Optimization (4-6h)

**Total: 32-41 hours**

---

### **Future (Backlog)**
1. ⚪ Note Linking & Graph
2. ⚪ Collaboration Features
3. ⚪ Notion Provider
4. ⚪ Wear OS
5. ⚪ Android TV
6. ⚪ Android Auto

---

## 📈 **Development Estimates**

### Phase 1: Production Ready (MVP+)
**Timeframe:** 2-3 weeks
- Runtime permissions
- Complete testing
- Error handling
- Note editing

**Result:** Production-ready app ready for Play Store

---

### Phase 2: Enhanced Experience
**Timeframe:** 1 month
- Voice transcription
- Home screen widget
- Export/Import
- UI polish

**Result:** Feature-complete with excellent UX

---

### Phase 3: Advanced Features
**Timeframe:** 2-3 months
- Rich text editor
- Image attachments
- Advanced search
- Note linking

**Result:** Power user features

---

### Phase 4: Multi-Platform
**Timeframe:** 3-6 months
- Wear OS
- Android TV
- Android Auto
- Ecosystem integration

**Result:** True multi-platform experience

---

## 🎯 **Immediate Next Steps**

### Week 1: Critical Path
1. ✅ Add runtime permission requests (Day 1-2)
2. ✅ Improve error handling UI (Day 2-3)
3. ✅ Complete repository tests (Day 3-4)
4. ✅ Complete ViewModel tests (Day 4-5)

### Week 2: Testing & Polish
1. ✅ Complete DAO tests (Day 1-2)
2. ✅ Complete UI tests (Day 3-4)
3. ✅ Add note editing (Day 5)

### Week 3: Features & Launch
1. ✅ Home screen widget (Day 1-3)
2. ✅ Final testing & bug fixes (Day 4-5)
3. ✅ Play Store preparation (Day 5)

---

## 📝 **Notes**

### Technical Debt
- None currently - clean architecture implemented

### Known Issues
- No runtime permission requests (declared only)
- No note editing screen
- Transcription not implemented (recording only)
- Provider sync is one-way (NoteDrop → Obsidian)

### Future Considerations
- Kotlin Multiplatform for shared code across platforms
- Jetpack Compose for Desktop (if expanding beyond Android)
- Server-side sync for cross-platform (optional, privacy-preserving)

---

## 🎉 **Progress Tracking**

### Overall Completion
- **MVP Features:** ✅ 95% Complete
- **Testing:** 🟡 35% Complete (69/220 tests)
- **Documentation:** ✅ 100% Complete
- **Production Ready:** 🟡 85% Complete

### Next Milestone
**Target:** Production-ready app on Play Store
**ETA:** 2-3 weeks
**Requirements:**
- ✅ Runtime permissions
- ✅ Complete testing
- ✅ Error handling
- ✅ Note editing
- ✅ Play Store assets

---

## 📞 **Questions to Answer**

Before proceeding with certain features, decide:

1. **Voice Transcription:**
   - Use cloud API (faster, smaller) or on-device Whisper (privacy, larger)?

2. **Monetization:**
   - One-time purchase as planned?
   - Which features in free vs pro?

3. **Platform Priority:**
   - Phone first, then which platform next?

4. **Provider Focus:**
   - Obsidian-first or multi-provider?

5. **Launch Timeline:**
   - Quick launch with core features or wait for more?

---

## 🚀 **Launch Checklist**

Before Play Store release:

### Required
- [ ] Runtime permissions implemented
- [ ] 95%+ test coverage
- [ ] Note editing works
- [ ] Error handling polished
- [ ] Privacy policy written
- [ ] Terms of service written
- [ ] Play Store listing created
- [ ] Screenshots prepared
- [ ] App icon finalized
- [ ] Release APK signed
- [ ] Beta testing completed

### Recommended
- [ ] Home screen widget
- [ ] Voice transcription
- [ ] Export/Import
- [ ] Onboarding flow
- [ ] Help documentation

---

**This roadmap is a living document. Update as priorities shift and features are completed!** 🗺️✨
