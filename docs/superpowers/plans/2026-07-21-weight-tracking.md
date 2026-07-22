# Weight Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user record one weight (in lbs) per day from a card on the Home screen, persisted indefinitely (exempt from the daily purge).

**Architecture:** New Room entity `WeightEntry` keyed by `LocalDate` (one row/day, overwrite on re-log), reached through `WeightDao` → `WeightRepository`. `HomeViewModel` folds today's weight into its existing `combine`, and a new `WeightCard` on the Home screen opens an `AlertDialog` to log/edit. `DailyResetWorker` is untouched, so weight is never purged.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room + KSP, Hilt DI, Coroutines/Flow. Tests: JUnit 5 (Jupiter) for ViewModel/repository, JUnit 4 + Robolectric for DAO, MockK, Turbine.

Design spec: `docs/superpowers/specs/2026-07-21-weight-tracking-design.md`

## Global Constraints

- Package root: `com.astute.calories`. Min SDK 26, target SDK 35, Java/Kotlin 17.
- **Only today's and yesterday's food data is retained.** Weight is a deliberate, documented exception — it is NOT purged.
- Every entity/schema change needs a new Room migration; never bump the DB version without one.
- Column SQL types must match the existing `Converters`: `LocalDate` → TEXT (`toString`), `Instant` → INTEGER (epoch millis), `Float` → REAL.
- Tests run on the JVM via `useJUnitPlatform()`; `unitTests.isReturnDefaultValues = true`. Follow existing test libraries (JUnit5/MockK/Turbine/Robolectric) — do not introduce new ones.
- **This machine cannot run `./gradlew`.** Where a step says "run the test", the executor verifies by careful reading/inspection if a build is unavailable, and records that. The gradle commands are still the canonical way to run them in CI or on a capable machine.

## Migration-test note (read before Task 1)

The two existing migrations (`MIGRATION_1_2`, `MIGRATION_2_3`) have **no tests**, and `AppDatabase` uses `exportSchema = false`. `MigrationTestHelper` requires an exported `3.json` schema to migrate *from*; none exists, and enabling export now only produces `4.json` going forward. A `MigrationTestHelper` 3→4 test is therefore not viable without hand-authoring schema JSON, which is out of scope and inconsistent with the repo's current approach.

**Coverage decision:** No standalone `MigrationTestHelper` test (consistent with the two existing untested migrations). Migration correctness is protected by (a) matching the DDL column types to `Converters` verbatim, and (b) `WeightDaoTest` opening a real `AppDatabase` at v4 — Room validates the `weight_entries` schema against the entity on open, so an entity/DDL mismatch surfaces there. Purge-exemption is covered by an explicit test in `WeightDaoTest` (Task 1, Step 8).

---

### Task 1: Persistence layer (entity, DAO, DB v4, DI wiring)

**Files:**
- Create: `app/src/main/java/com/astute/calories/data/local/entity/WeightEntry.kt`
- Create: `app/src/main/java/com/astute/calories/data/local/dao/WeightDao.kt`
- Modify: `app/src/main/java/com/astute/calories/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/astute/calories/di/AppModule.kt`
- Modify: `app/src/main/java/com/astute/calories/worker/DailyResetWorker.kt`
- Test: `app/src/test/java/com/astute/calories/data/local/dao/WeightDaoTest.kt`

**Interfaces:**
- Produces:
  - `WeightEntry(date: LocalDate, weightLbs: Float, recordedAt: Instant)` — `@PrimaryKey val date`.
  - `WeightDao`: `suspend fun upsert(entry: WeightEntry)`, `fun getForDate(date: LocalDate): Flow<WeightEntry?>`, `fun getRecent(): Flow<List<WeightEntry>>`, `suspend fun delete(date: LocalDate)`.
  - `AppDatabase.weightDao(): WeightDao`, `AppDatabase.MIGRATION_3_4`.
  - Hilt provider `provideWeightDao(db): WeightDao`.

- [ ] **Step 1: Create the entity**

`app/src/main/java/com/astute/calories/data/local/entity/WeightEntry.kt`:

```kotlin
package com.astute.calories.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val date: LocalDate,   // one row per day; re-logging overwrites
    val weightLbs: Float,
    val recordedAt: Instant
)
```

- [ ] **Step 2: Create the DAO**

`app/src/main/java/com/astute/calories/data/local/dao/WeightDao.kt`:

```kotlin
package com.astute.calories.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntry)

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    fun getForDate(date: LocalDate): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entries ORDER BY date DESC")
    fun getRecent(): Flow<List<WeightEntry>>

    @Query("DELETE FROM weight_entries WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
```

- [ ] **Step 3: Update `AppDatabase` — version 4, entity, DAO, migration**

In `app/src/main/java/com/astute/calories/data/local/AppDatabase.kt`:

Add imports:
```kotlin
import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.WeightEntry
```

Change the `@Database` annotation and add the DAO accessor:
```kotlin
@Database(
    entities = [LogEntry::class, CachedFood::class, SavedMeal::class, WeightEntry::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun foodCacheDao(): FoodCacheDao
    abstract fun savedMealDao(): SavedMealDao
    abstract fun weightDao(): WeightDao
```

Add `MIGRATION_3_4` inside the `companion object`, after `MIGRATION_2_3`:
```kotlin
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // date TEXT (LocalDate.toString), weightLbs REAL (Float),
                // recordedAt INTEGER (Instant epoch millis) — matches Converters.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS weight_entries (" +
                        "date TEXT NOT NULL PRIMARY KEY, " +
                        "weightLbs REAL NOT NULL, " +
                        "recordedAt INTEGER NOT NULL)"
                )
            }
        }
```

- [ ] **Step 4: Register the migration and DAO provider in `AppModule`**

In `app/src/main/java/com/astute/calories/di/AppModule.kt`:

Add import:
```kotlin
import com.astute.calories.data.local.dao.WeightDao
```

Update `.addMigrations(...)` to include the new migration:
```kotlin
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4
        ).build()
```

Add the DAO provider next to the others:
```kotlin
    @Provides
    fun provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()
```

- [ ] **Step 5: Document the purge exemption in `DailyResetWorker`**

In `app/src/main/java/com/astute/calories/worker/DailyResetWorker.kt`, add a comment above the existing delete call inside `doWork()`:

```kotlin
        val yesterday = LocalDate.now().minusDays(1)
        // Delete anything older than yesterday.
        // NOTE: weight_entries is intentionally EXEMPT from the purge — weight is
        // retained indefinitely so trends stay possible. Do not add weight cleanup here.
        dailyLogRepository.deleteEntriesBefore(yesterday)
```

- [ ] **Step 6: Write the failing DAO test**

`app/src/test/java/com/astute/calories/data/local/dao/WeightDaoTest.kt`:

```kotlin
package com.astute.calories.data.local.dao

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.astute.calories.data.local.AppDatabase
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.MealCategory
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class WeightDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WeightDao

    private val recordedAt = Instant.parse("2025-01-15T08:00:00Z")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.weightDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun weight(date: LocalDate, lbs: Float) =
        WeightEntry(date = date, weightLbs = lbs, recordedAt = recordedAt)

    @Test
    fun `upsert then getForDate returns the entry`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))

        val result = dao.getForDate(date).first()

        assertEquals(178.4f, result?.weightLbs)
        assertEquals(date, result?.date)
    }

    @Test
    fun `re-upserting the same date overwrites and keeps one row`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))
        dao.upsert(weight(date, 176.0f))

        val all = dao.getRecent().first()
        assertEquals(1, all.size)
        assertEquals(176.0f, all.single().weightLbs)
    }

    @Test
    fun `getForDate returns null when no entry exists`() = runBlocking {
        assertNull(dao.getForDate(LocalDate.of(2025, 1, 15)).first())
    }

    @Test
    fun `getRecent returns entries ordered by date descending`() = runBlocking {
        dao.upsert(weight(LocalDate.of(2025, 1, 13), 180f))
        dao.upsert(weight(LocalDate.of(2025, 1, 15), 178f))
        dao.upsert(weight(LocalDate.of(2025, 1, 14), 179f))

        val dates = dao.getRecent().first().map { it.date }

        assertEquals(
            listOf(
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 1, 14),
                LocalDate.of(2025, 1, 13)
            ),
            dates
        )
    }

    @Test
    fun `delete removes the entry for that date`() = runBlocking {
        val date = LocalDate.of(2025, 1, 15)
        dao.upsert(weight(date, 178.4f))

        dao.delete(date)

        assertNull(dao.getForDate(date).first())
    }

    @Test
    fun `weight survives a daily-log purge (exemption guard)`() = runBlocking {
        // DailyResetWorker calls DailyLogDao.deleteEntriesBefore, which must NOT touch weight.
        val twoDaysAgo = LocalDate.of(2025, 1, 13)
        val yesterday = LocalDate.of(2025, 1, 14)
        dao.upsert(weight(twoDaysAgo, 180f))
        db.dailyLogDao().insert(
            LogEntry(
                date = twoDaysAgo,
                mealCategory = MealCategory.SNACKS,
                foodName = "Old",
                calories = 100,
                proteinG = 0f,
                carbsG = 0f,
                fatG = 0f,
                servingSize = 0f,
                quantity = 1f,
                addedAt = recordedAt
            )
        )

        db.dailyLogDao().deleteEntriesBefore(yesterday)

        // Old log entry is gone, but the weight from the same day survives.
        assertEquals(0, db.dailyLogDao().getEntriesForDate(twoDaysAgo).first().size)
        assertEquals(180f, dao.getForDate(twoDaysAgo).first()?.weightLbs)
    }
}
```

- [ ] **Step 7: Run the DAO test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astute.calories.data.local.dao.WeightDaoTest"`
Expected: PASS (6 tests). If gradle is unavailable, verify by inspection that the entity/DAO/DB compile together and the assertions match the DAO contract, and record that the run was skipped.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/astute/calories/data/local/entity/WeightEntry.kt \
        app/src/main/java/com/astute/calories/data/local/dao/WeightDao.kt \
        app/src/main/java/com/astute/calories/data/local/AppDatabase.kt \
        app/src/main/java/com/astute/calories/di/AppModule.kt \
        app/src/main/java/com/astute/calories/worker/DailyResetWorker.kt \
        app/src/test/java/com/astute/calories/data/local/dao/WeightDaoTest.kt
git commit -m "Add WeightEntry persistence (entity, DAO, DB v4 migration)"
```

---

### Task 2: WeightRepository

**Files:**
- Create: `app/src/main/java/com/astute/calories/data/repository/WeightRepository.kt`
- Test: `app/src/test/java/com/astute/calories/data/repository/WeightRepositoryTest.kt`

**Interfaces:**
- Consumes: `WeightDao` (Task 1).
- Produces: `WeightRepository` (`@Singleton`, `@Inject constructor(dao: WeightDao)`) with:
  - `suspend fun upsert(entry: WeightEntry)`
  - `fun getForDate(date: LocalDate): Flow<WeightEntry?>`
  - `fun getRecent(): Flow<List<WeightEntry>>`
  - `suspend fun delete(date: LocalDate)`

- [ ] **Step 1: Write the failing repository test**

`app/src/test/java/com/astute/calories/data/repository/WeightRepositoryTest.kt`:

```kotlin
package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.WeightEntry
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class WeightRepositoryTest {

    private lateinit var dao: WeightDao
    private lateinit var repository: WeightRepository

    private val today = LocalDate.of(2025, 1, 15)
    private val sample = WeightEntry(
        date = today,
        weightLbs = 178.4f,
        recordedAt = Instant.parse("2025-01-15T08:00:00Z")
    )

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = WeightRepository(dao)
    }

    @Test
    fun `getForDate returns entry from dao`() = runTest {
        every { dao.getForDate(today) } returns flowOf(sample)

        val result = repository.getForDate(today).first()

        assertEquals(178.4f, result?.weightLbs)
    }

    @Test
    fun `getRecent returns entries from dao`() = runTest {
        every { dao.getRecent() } returns flowOf(listOf(sample))

        val result = repository.getRecent().first()

        assertEquals(1, result.size)
    }

    @Test
    fun `upsert calls dao upsert`() = runTest {
        repository.upsert(sample)

        coVerify { dao.upsert(sample) }
    }

    @Test
    fun `delete calls dao delete`() = runTest {
        repository.delete(today)

        coVerify { dao.delete(today) }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astute.calories.data.repository.WeightRepositoryTest"`
Expected: FAIL — `WeightRepository` does not exist (unresolved reference). If gradle is unavailable, confirm the class is absent.

- [ ] **Step 3: Implement `WeightRepository`**

`app/src/main/java/com/astute/calories/data/repository/WeightRepository.kt`:

```kotlin
package com.astute.calories.data.repository

import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepository @Inject constructor(
    private val weightDao: WeightDao
) {
    fun getForDate(date: LocalDate): Flow<WeightEntry?> =
        weightDao.getForDate(date)

    fun getRecent(): Flow<List<WeightEntry>> =
        weightDao.getRecent()

    suspend fun upsert(entry: WeightEntry) =
        weightDao.upsert(entry)

    suspend fun delete(date: LocalDate) =
        weightDao.delete(date)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astute.calories.data.repository.WeightRepositoryTest"`
Expected: PASS (4 tests). If gradle is unavailable, verify by inspection the repository delegates each call to the DAO.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/astute/calories/data/repository/WeightRepository.kt \
        app/src/test/java/com/astute/calories/data/repository/WeightRepositoryTest.kt
git commit -m "Add WeightRepository over WeightDao"
```

---

### Task 3: HomeViewModel wiring

**Files:**
- Modify: `app/src/main/java/com/astute/calories/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/astute/calories/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `WeightRepository` (Task 2), `WeightEntry` (Task 1).
- Produces:
  - `HomeUiState.todayWeight: WeightEntry?` (default `null`).
  - `HomeViewModel.logWeight(lbs: Float)`.
  - `HomeViewModel` constructor now takes `weightRepository: WeightRepository` as a 4th parameter.

- [ ] **Step 1: Add failing tests to `HomeViewModelTest`**

In `app/src/test/java/com/astute/calories/ui/home/HomeViewModelTest.kt`:

Add imports:
```kotlin
import com.astute.calories.data.local.entity.WeightEntry
import com.astute.calories.data.repository.WeightRepository
```

Add a field alongside the other repositories:
```kotlin
    private lateinit var weightRepository: WeightRepository
```

In `setup()`, create and stub it (add after the `savedMealRepository` stub):
```kotlin
        weightRepository = mockk(relaxed = true)
        every { weightRepository.getForDate(any()) } returns flowOf(null)
```

Update `createViewModel()`:
```kotlin
    private fun createViewModel() =
        HomeViewModel(dailyLogRepository, savedMealRepository, userPreferences, weightRepository)
```

Add two tests:
```kotlin
    @Test
    fun `uiState surfaces today's weight`() = runTest {
        val entry = WeightEntry(
            date = LocalDate.now(),
            weightLbs = 178.4f,
            recordedAt = Instant.now()
        )
        every { weightRepository.getForDate(any()) } returns flowOf(entry)
        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // skip initial
            val state = awaitItem()
            assertEquals(178.4f, state.todayWeight?.weightLbs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logWeight upserts entry for today`() = runTest {
        viewModel = createViewModel()

        viewModel.logWeight(176.2f)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            weightRepository.upsert(match {
                it.date == LocalDate.now() && it.weightLbs == 176.2f
            })
        }
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astute.calories.ui.home.HomeViewModelTest"`
Expected: FAIL — `HomeViewModel` has no 4th parameter / no `logWeight` / `HomeUiState` has no `todayWeight`. If gradle is unavailable, confirm those symbols are absent.

- [ ] **Step 3: Wire `WeightRepository` into `HomeViewModel`**

In `app/src/main/java/com/astute/calories/ui/home/HomeViewModel.kt`:

Add imports:
```kotlin
import com.astute.calories.data.local.entity.WeightEntry
import com.astute.calories.data.repository.WeightRepository
```

Add `todayWeight` to `HomeUiState`:
```kotlin
data class HomeUiState(
    val date: LocalDate = LocalDate.now(),
    val calorieGoal: Int = 2000,
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val entriesByCategory: Map<MealCategory, List<LogEntry>> = emptyMap(),
    val savedMealsByCategory: Map<MealCategory, List<SavedMeal>> = emptyMap(),
    val todayWeight: WeightEntry? = null
)
```

Add the constructor parameter:
```kotlin
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val savedMealRepository: SavedMealRepository,
    private val userPreferences: UserPreferences,
    private val weightRepository: WeightRepository
) : ViewModel() {
```

Add the weight flow to the `combine` (5 flows now) and set `todayWeight`:
```kotlin
    val uiState: StateFlow<HomeUiState> = combine(
        today,
        dailyLogRepository.getEntriesForDate(LocalDate.now()),
        userPreferences.calorieGoal,
        savedMealRepository.getAll(),
        weightRepository.getForDate(LocalDate.now())
    ) { date, entries, goal, savedMeals, todayWeight ->
        val grouped = entries.groupBy { it.mealCategory }
        val savedGrouped = savedMeals.groupBy { it.category }
        HomeUiState(
            date = date,
            calorieGoal = goal,
            totalCalories = entries.sumOf { it.calories },
            totalProtein = entries.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalCarbs = entries.sumOf { it.carbsG.toDouble() }.toFloat(),
            totalFat = entries.sumOf { it.fatG.toDouble() }.toFloat(),
            entriesByCategory = grouped,
            savedMealsByCategory = savedGrouped,
            todayWeight = todayWeight
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
```

Add `logWeight` (place it after `copyYesterday`, before the closing brace):
```kotlin
    fun logWeight(lbs: Float) {
        viewModelScope.launch {
            weightRepository.upsert(
                WeightEntry(
                    date = LocalDate.now(),
                    weightLbs = lbs,
                    recordedAt = Instant.now()
                )
            )
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.astute.calories.ui.home.HomeViewModelTest"`
Expected: PASS (all existing tests plus the 2 new ones). If gradle is unavailable, verify by inspection that the 5-arg `combine` overload is used, `todayWeight` is populated, and `logWeight` builds the entry with `LocalDate.now()`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/astute/calories/ui/home/HomeViewModel.kt \
        app/src/test/java/com/astute/calories/ui/home/HomeViewModelTest.kt
git commit -m "Surface today's weight and logWeight in HomeViewModel"
```

---

### Task 4: WeightCard UI and Home screen integration

**Files:**
- Create: `app/src/main/java/com/astute/calories/ui/home/components/WeightCard.kt`
- Modify: `app/src/main/java/com/astute/calories/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `HomeUiState.todayWeight` and `HomeViewModel.logWeight(Float)` (Task 3).
- Produces: `WeightCard(todayWeight: WeightEntry?, onLogWeight: (Float) -> Unit, modifier: Modifier = Modifier)` — a self-contained composable that owns its own log-dialog state.

No automated test — consistent with the current suite (no Compose UI test infrastructure exists). Verify manually per Step 3.

- [ ] **Step 1: Create the `WeightCard` composable (card + log dialog)**

`app/src/main/java/com/astute/calories/ui/home/components/WeightCard.kt`:

```kotlin
package com.astute.calories.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.astute.calories.data.local.entity.WeightEntry

@Composable
fun WeightCard(
    todayWeight: WeightEntry?,
    onLogWeight: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = todayWeight
                    ?.let { "%.1f lbs".format(it.weightLbs) }
                    ?: "Log today's weight",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }

    if (showDialog) {
        WeightLogDialog(
            initialWeight = todayWeight?.weightLbs,
            onDismiss = { showDialog = false },
            onConfirm = { lbs ->
                onLogWeight(lbs)
                showDialog = false
            }
        )
    }
}

@Composable
private fun WeightLogDialog(
    initialWeight: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember {
        mutableStateOf(initialWeight?.let { "%.1f".format(it) } ?: "")
    }
    val parsed = text.trim().toFloatOrNull()
    val isValid = parsed != null && parsed > 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Weight (lbs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
```

- [ ] **Step 2: Add `WeightCard` to the Home screen**

In `app/src/main/java/com/astute/calories/ui/home/HomeScreen.kt`:

Add import:
```kotlin
import com.astute.calories.ui.home.components.WeightCard
```

Insert a new `item { }` in the `LazyColumn` immediately after the `MacroSummary` item (after its closing `}` on the block that renders `MacroSummary`, before the "Copy yesterday's meals" `item`):
```kotlin
            item {
                WeightCard(
                    todayWeight = uiState.todayWeight,
                    onLogWeight = { viewModel.logWeight(it) }
                )
            }
```

- [ ] **Step 3: Verify the build and screen manually**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL. If gradle is unavailable, verify by inspection that imports resolve, `WeightCard` parameters match the call site, and the `Card(onClick = ...)` overload is the Material 3 experimental one (the file already uses `@OptIn(ExperimentalMaterial3Api::class)` at the composable — confirm it covers `HomeScreen`; `WeightCard.kt` uses the stable `Card(onClick=)` which is not experimental in current Material 3, so no opt-in needed there).

Manual check (on a device/emulator, if available): Home shows a Weight card after the macro summary; tapping opens the dialog; entering a value and Save shows "NNN.N lbs"; reopening pre-fills the value; empty/invalid input disables Save.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/astute/calories/ui/home/components/WeightCard.kt \
        app/src/main/java/com/astute/calories/ui/home/HomeScreen.kt
git commit -m "Add WeightCard to Home screen with log dialog"
```

---

## Final verification

- [ ] Run the full unit suite: `./gradlew :app:testDebugUnitTest` — expected PASS (new: 6 DAO + 4 repository + 2 ViewModel tests). If gradle is unavailable, record that verification was by inspection.
- [ ] Run lint: `./gradlew lint` — expected no new errors.
- [ ] Confirm `DailyResetWorker` still references only `dailyLogRepository` (weight untouched).
- [ ] Update `CLAUDE.md` if desired: note the 4th entity `WeightEntry`, DB now at version 4, and that weight is the documented exception to the "today + yesterday only" purge rule. (Optional; not required for the feature to work.)
