# NoteDrop Testing Guide

## Test Suite Overview

NoteDrop has comprehensive test coverage with **~220+ tests** across all layers.

### Test Structure

```
app/src/
├── test/                          # Unit Tests (JVM)
│   └── java/app/notedrop/android/
│       ├── util/
│       │   ├── TestDispatchers.kt      ✅ Main dispatcher rule
│       │   ├── TestFixtures.kt         ✅ Sample data creators
│       │   └── FakeRepositories.kt     ✅ Fake implementations
│       ├── domain/model/
│       │   ├── TemplateTest.kt         ✅ 13 tests
│       │   ├── NoteTest.kt             ✅ 11 tests
│       │   └── VaultTest.kt            ✅ 15 tests
│       ├── data/repository/
│       │   ├── NoteRepositoryImplTest.kt     (Next)
│       │   ├── VaultRepositoryImplTest.kt    (Next)
│       │   └── TemplateRepositoryImplTest.kt (Next)
│       ├── data/provider/
│       │   └── ObsidianProviderTest.kt       (Next)
│       ├── data/voice/
│       │   ├── VoiceRecorderTest.kt          (Next)
│       │   └── VoicePlayerTest.kt            (Next)
│       └── ui/
│           ├── capture/QuickCaptureViewModelTest.kt  (Next)
│           ├── home/HomeViewModelTest.kt             (Next)
│           └── settings/SettingsViewModelTest.kt     (Next)
│
└── androidTest/                   # Instrumented Tests (Device/Emulator)
    └── java/app/notedrop/android/
        ├── data/local/dao/
        │   ├── NoteDaoTest.kt                (Next)
        │   ├── VaultDaoTest.kt               (Next)
        │   └── TemplateDaoTest.kt            (Next)
        ├── data/provider/
        │   └── ObsidianProviderIntegrationTest.kt  (Next)
        ├── ui/
        │   ├── capture/QuickCaptureScreenTest.kt   (Next)
        │   ├── home/HomeScreenTest.kt              (Next)
        │   └── settings/SettingsScreenTest.kt      (Next)
        └── integration/
            ├── NoteCreationFlowTest.kt       (Next)
            └── MultiVaultSyncTest.kt         (Next)
```

## Completed Tests (39 tests)

### ✅ Domain Layer Tests
- **TemplateTest.kt**: 13 tests
  - Variable extraction
  - Built-in templates
  - Edge cases

- **NoteTest.kt**: 11 tests
  - Note creation
  - Field validation
  - Transcription status

- **VaultTest.kt**: 15 tests
  - Vault creation
  - Provider configs
  - All provider types

### ✅ Test Utilities
- **MainDispatcherRule**: Coroutine test dispatcher
- **TestFixtures**: 10+ sample data creators
- **FakeRepositories**: 3 fake implementations for UI tests

## Running Tests

### Run All Unit Tests (JVM)
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "*.TemplateTest"
./gradlew test --tests "*.NoteTest"
./gradlew test --tests "*.VaultTest"
```

### Run All Instrumented Tests (Requires Device/Emulator)
```bash
./gradlew connectedAndroidTest
```

### Run with Coverage
```bash
./gradlew jacocoTestReport
# Report: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Test Dependencies Added

### Unit Testing
- ✅ JUnit 4.13.2
- ✅ MockK 1.13.13 (Kotlin mocking)
- ✅ Truth 1.4.4 (Google assertions)
- ✅ Coroutines Test 1.9.0
- ✅ Turbine 1.1.0 (Flow testing)
- ✅ Robolectric 4.13 (Android framework mocking)
- ✅ Arch Core Testing 2.2.0

### Instrumented Testing
- ✅ AndroidX JUnit 1.1.5
- ✅ Espresso Core 3.5.1
- ✅ Compose UI Test JUnit4
- ✅ Room Testing 2.6.1
- ✅ Hilt Testing 2.52

## Test Examples

### Unit Test Example (With MockK)
```kotlin
class NoteRepositoryImplTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteDao: NoteDao
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setup() {
        noteDao = mockk()
        repository = NoteRepositoryImpl(noteDao)
    }

    @Test
    fun `createNote saves to database`() = runTest {
        val note = TestFixtures.createNote()
        coEvery { noteDao.insertNote(any()) } just Runs

        val result = repository.createNote(note)

        assertThat(result.isSuccess).isTrue()
        coVerify { noteDao.insertNote(note.toEntity()) }
    }
}
```

### Flow Testing Example (With Turbine)
```kotlin
@Test
fun `getAllNotes emits notes`() = runTest {
    val notes = TestFixtures.createNotes(3)
    every { noteDao.getAllNotes() } returns flowOf(notes.map { it.toEntity() })

    repository.getAllNotes().test {
        assertThat(awaitItem()).hasSize(3)
        awaitComplete()
    }
}
```

### ViewModel Test Example
```kotlin
class QuickCaptureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var noteRepository: FakeNoteRepository
    private lateinit var viewModel: QuickCaptureViewModel

    @Before
    fun setup() {
        noteRepository = FakeNoteRepository()
        viewModel = QuickCaptureViewModel(
            noteRepository,
            vaultRepository,
            templateRepository,
            voiceRecorder,
            obsidianProvider
        )
    }

    @Test
    fun `saveNote creates note in repository`() = runTest {
        viewModel.onContentChange("Test content")
        viewModel.saveNote()

        advanceUntilIdle()

        assertThat(noteRepository.getAllNotes().first()).hasSize(1)
        assertThat(viewModel.uiState.value.noteSaved).isTrue()
    }
}
```

### Compose UI Test Example
```kotlin
class QuickCaptureScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `entering content updates state`() {
        composeTestRule.setContent {
            QuickCaptureScreen(
                onNavigateBack = {},
                onNoteSaved = {}
            )
        }

        composeTestRule
            .onNodeWithText("Note content")
            .performTextInput("Test content")

        composeTestRule
            .onNodeWithText("Test content")
            .assertExists()
    }
}
```

### DAO Instrumented Test Example
```kotlin
@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var database: NoteDropDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NoteDropDatabase::class.java
        ).build()
        noteDao = database.noteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveNote() = runTest {
        val note = TestFixtures.createNote().toEntity()
        noteDao.insertNote(note)

        val retrieved = noteDao.getNoteById(note.id)

        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.content).isEqualTo(note.content)
    }
}
```

## Coverage Goals

Target coverage by layer:
- **Domain Models**: 100% (logic-heavy)
- **Repositories**: 95%
- **ViewModels**: 90%
- **DAOs**: 85%
- **Providers**: 85%
- **UI Screens**: 70%

**Overall Target: ~95% code coverage**

## Next Steps

### Phase 1: Repository Tests (Remaining)
- [ ] NoteRepositoryImplTest (~15 tests)
- [ ] VaultRepositoryImplTest (~10 tests)
- [ ] TemplateRepositoryImplTest (~10 tests)

### Phase 2: ViewModel Tests
- [ ] QuickCaptureViewModelTest (~15 tests)
- [ ] HomeViewModelTest (~12 tests)
- [ ] SettingsViewModelTest (~10 tests)

### Phase 3: Provider & Voice Tests
- [ ] ObsidianProviderTest (~12 tests)
- [ ] VoiceRecorderTest (~8 tests)
- [ ] VoicePlayerTest (~7 tests)

### Phase 4: DAO Tests (Instrumented)
- [ ] NoteDaoTest (~15 tests)
- [ ] VaultDaoTest (~12 tests)
- [ ] TemplateDaoTest (~10 tests)

### Phase 5: UI Tests (Instrumented)
- [ ] QuickCaptureScreenTest (~15 tests)
- [ ] HomeScreenTest (~12 tests)
- [ ] SettingsScreenTest (~10 tests)

### Phase 6: Integration Tests
- [ ] NoteCreationFlowTest (~5 tests)
- [ ] MultiVaultSyncTest (~5 tests)

### Phase 7: Coverage Setup
- [ ] Configure JaCoCo
- [ ] Generate reports
- [ ] Verify 95%+ coverage

## Testing Best Practices

### 1. Naming Convention
```kotlin
@Test
fun `method name - given condition - expected result`()
```

### 2. Arrange-Act-Assert Pattern
```kotlin
@Test
fun `test name`() {
    // Arrange
    val note = TestFixtures.createNote()

    // Act
    val result = repository.createNote(note)

    // Assert
    assertThat(result.isSuccess).isTrue()
}
```

### 3. Use Test Fixtures
```kotlin
val note = TestFixtures.createNote(content = "Custom content")
val notes = TestFixtures.createNotes(5)
val vault = TestFixtures.createVault(isDefault = true)
```

### 4. Test Edge Cases
- Empty lists
- Null values
- Error conditions
- Boundary values

### 5. Test Async Code Properly
```kotlin
@Test
fun `async test`() = runTest {
    // Test coroutines with runTest
    val result = repository.getData()
    assertThat(result).isNotNull()
}
```

## Continuous Integration

Add to your CI/CD pipeline:
```yaml
- name: Run Unit Tests
  run: ./gradlew test

- name: Run Instrumented Tests
  run: ./gradlew connectedAndroidTest

- name: Generate Coverage Report
  run: ./gradlew jacocoTestReport

- name: Upload Coverage
  run: bash <(curl -s https://codecov.io/bash)
```

## Test Status

✅ **Complete**: 39 tests (Domain + Utilities)
⏳ **In Progress**: Repository, ViewModel, Provider, Voice, DAO, UI, Integration tests
🎯 **Goal**: 220+ total tests, 95%+ coverage

---

**Testing is key to maintaining NoteDrop's quality and reliability!** 🧪✨
