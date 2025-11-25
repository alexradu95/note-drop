# Next Steps After Quick Wins

## ⚠️ Important: Repository Implementations Need Updates

The repository **interfaces** have been updated to use `Result<T, AppError>`, but the **implementations** will have compilation errors until updated.

---

## 🔴 CRITICAL - Fix Compilation Errors

You will see compilation errors in these files when you sync Gradle:

### 1. NoteRepositoryImpl
**File**: `app/src/main/java/app/notedrop/android/data/repository/NoteRepositoryImpl.kt`

**What to do**: Update method signatures to return `Result<T, AppError>` instead of `kotlin.Result<T>` or nullable types.

**Example migration**:
```kotlin
// Before
override suspend fun getNoteById(id: String): Note? {
    return withContext(ioDispatcher) {
        noteDao.getNoteById(id)?.toDomain()
    }
}

// After
override suspend fun getNoteById(id: String): Result<Note, AppError> {
    return withContext(ioDispatcher) {
        databaseResultOf {
            noteDao.getNoteById(id)
        }.andThen { entity ->
            entity.toResultOrNotFound("Note", id)
        }.map { it.toDomain() }
    }
}
```

### 2. VaultRepositoryImpl
**File**: `app/src/main/java/app/notedrop/android/data/repository/VaultRepositoryImpl.kt`

**What to do**: Same as NoteRepositoryImpl.

### 3. TemplateRepositoryImpl
**File**: `app/src/main/java/app/notedrop/android/data/repository/TemplateRepositoryImpl.kt`

**Optional**: Consider updating to use Result types as well for consistency.

### 4. ViewModels
**Files**:
- `app/src/main/java/app/notedrop/android/ui/capture/QuickCaptureViewModel.kt`
- `app/src/main/java/app/notedrop/android/ui/home/HomeViewModel.kt`
- `app/src/main/java/app/notedrop/android/ui/settings/SettingsViewModel.kt`

**What to do**: Update to handle new Result types.

**Example**:
```kotlin
// Before
viewModelScope.launch {
    val note = noteRepository.createNote(newNote).getOrNull()
    if (note != null) {
        _uiState.value = UiState.Success(note)
    } else {
        _uiState.value = UiState.Error("Failed to save note")
    }
}

// After
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.onFailure
import app.notedrop.android.domain.model.toUserMessage

viewModelScope.launch {
    noteRepository.createNote(newNote)
        .onSuccess { note ->
            _uiState.value = UiState.Success(note)
        }
        .onFailure { error ->
            _uiState.value = UiState.Error(error.toUserMessage())
        }
}
```

---

## 📋 Step-by-Step Guide

### Step 1: Sync Gradle
```bash
# In Android Studio
File → Sync Project with Gradle Files

# Or from terminal
./gradlew build
```

You'll see compilation errors - this is expected!

### Step 2: Update NoteRepositoryImpl (30-45 min)
1. Open `NoteRepositoryImpl.kt`
2. Add imports:
   ```kotlin
   import com.github.michaelbull.result.Result
   import app.notedrop.android.domain.model.AppError
   import app.notedrop.android.util.*
   ```
3. Update each method one by one
4. Use the helper functions from `ResultExtensions.kt`

### Step 3: Update VaultRepositoryImpl (30-45 min)
Same process as NoteRepositoryImpl.

### Step 4: Update ViewModels (1-2 hours)
1. Update QuickCaptureViewModel
2. Update HomeViewModel
3. Update SettingsViewModel
4. Test each screen manually

### Step 5: Run Tests and Fix (1-2 hours)
```bash
./gradlew test
./gradlew testDebugUnitTest
```

Update test expectations for new Result types.

### Step 6: Manual Testing
1. Run app in debug mode
2. Test note creation
3. Test note editing
4. Test vault creation
5. Check LeakCanary for memory leaks

---

## 🎯 After Repository Updates Are Complete

Once repositories compile successfully, proceed with these improvements:

### Phase 1: Use Case Layer (High Priority)
Create use cases to separate business logic from ViewModels:

```
domain/usecase/
  note/
    CreateNoteUseCase.kt
    UpdateNoteUseCase.kt
    DeleteNoteUseCase.kt
    GetNotesForTodayUseCase.kt
  vault/
    CreateVaultUseCase.kt
    SetDefaultVaultUseCase.kt
  sync/
    SyncVaultUseCase.kt
```

**Estimated time**: 1-2 weeks

### Phase 2: SyncCoordinator Implementation (Critical)
Implement the SyncCoordinator interface with:
- WorkManager integration
- Retry logic with exponential backoff
- Conflict resolution using ConflictResolver
- Background sync scheduling

**Estimated time**: 2-3 weeks

### Phase 3: Integration Testing (High Priority)
Add integration tests using Kaspresso:

```kotlin
dependencies {
    androidTestImplementation("com.kaspersky.android-components:kaspresso:1.5.5")
    androidTestImplementation("com.kaspersky.android-components:kaspresso-compose-support:1.5.5")
}
```

**Estimated time**: 1-2 weeks

---

## 🧪 Testing Checklist

After updating repositories:

- [ ] App compiles without errors
- [ ] Unit tests pass
- [ ] Can create a note
- [ ] Can update a note
- [ ] Can delete a note
- [ ] Can create a vault
- [ ] Can set default vault
- [ ] Error messages show correctly in UI
- [ ] LeakCanary doesn't report leaks
- [ ] No crashes during normal usage

---

## 📚 Learning Resources

### kotlin-result
- [GitHub](https://github.com/michaelbull/kotlin-result)
- [API Docs](https://github.com/michaelbull/kotlin-result/blob/master/README.md)

**Key functions to know**:
- `Ok(value)` - Create success result
- `Err(error)` - Create error result
- `.onSuccess { }` - Handle success
- `.onFailure { }` - Handle error
- `.map { }` - Transform success value
- `.mapError { }` - Transform error
- `.andThen { }` - Chain operations (flatMap)
- `.recover { }` - Provide fallback for errors

### LeakCanary
- [Setup Guide](https://square.github.io/leakcanary/getting_started/)
- [FAQ](https://square.github.io/leakcanary/faq/)

### Dokka
- [Getting Started](https://kotlinlang.org/docs/dokka-get-started.html)
- [KDoc Syntax](https://kotlinlang.org/docs/kotlin-doc.html)

---

## 🆘 Troubleshooting

### "Unresolved reference: Result"
**Solution**: Make sure you import `com.github.michaelbull.result.Result`, not `kotlin.Result`

### "Type mismatch: inferred type is kotlin.Result but Result<T, AppError> was expected"
**Solution**: Use `.toAppResult()` extension to convert:
```kotlin
val kotlinResult: kotlin.Result<Note> = legacyFunction()
val result: Result<Note, AppError> = kotlinResult.toAppResult()
```

### LeakCanary shows false positives
**Solution**: Check [LeakCanary FAQ](https://square.github.io/leakcanary/faq/) for known issues

### Dokka fails to generate docs
**Solution**:
```bash
./gradlew clean
./gradlew dokkaHtml
```

---

## 💡 Pro Tips

1. **Start small**: Update one repository method at a time
2. **Use type inference**: Let Kotlin infer Result types when possible
3. **Test incrementally**: Run tests after each repository update
4. **Use binding**: For multiple Result operations, use binding blocks:
   ```kotlin
   binding {
       val vault = vaultRepository.getDefaultVault().bind()
       val note = noteRepository.createNote(newNote).bind()
       // Both succeeded
   }
   ```
5. **Error messages matter**: Use `toUserMessage()` consistently for UI

---

## 📊 Progress Tracking

### Quick Wins ✅
- [x] LeakCanary added
- [x] Database migration fixed
- [x] Dokka configured
- [x] kotlin-result added
- [x] AppError created
- [x] Repositories updated (interfaces only)
- [x] ResultExtensions created

### Next (Critical) ⏳
- [ ] NoteRepositoryImpl updated
- [ ] VaultRepositoryImpl updated
- [ ] ViewModels updated
- [ ] Tests updated
- [ ] Manual testing complete

### Future (Important) 📅
- [ ] Use Case layer added
- [ ] SyncCoordinator implemented
- [ ] Integration tests added
- [ ] Performance profiled

---

## 🎉 When You're Done

Once all repositories and ViewModels are updated:

1. **Commit your changes**:
   ```bash
   git add .
   git commit -m "feat: implement error handling with kotlin-result

   - Add kotlin-result library for type-safe error handling
   - Create AppError sealed class hierarchy
   - Update repositories to use Result<T, AppError>
   - Add ResultExtensions helpers
   - Fix database migration strategy for production
   - Add LeakCanary for memory leak detection
   - Configure Dokka for documentation generation
   "
   ```

2. **Generate documentation**:
   ```bash
   ./gradlew dokkaHtml
   ```

3. **Run full test suite**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

4. **Profile the app** with Android Profiler and check LeakCanary

5. **Move to next phase**: Use Case layer or SyncCoordinator implementation

---

**Good luck! The foundation is now much stronger.** 🚀
