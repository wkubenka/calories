# Weight Tracking — Design

**Date:** 2026-07-21
**Status:** Approved (pending spec review)

## Goal

Add a very basic weight tracking feature: record a weight for a date. Statistics,
charts, and trends are explicitly out of scope for now, but the data model must
not preclude them later.

## Product constraint interaction

The app's defining rule is that **only today's and yesterday's data is retained**
(`DailyResetWorker` purges `log_entries` older than yesterday). Weight is the one
feature where that rule conflicts with the feature's purpose, since weight is only
meaningful as a trend over time.

**Decision: weight is exempt from the daily purge.** Weight entries persist
indefinitely. This is a deliberate, documented exception to the core constraint,
and it is what makes future statistics possible.

## Decisions

- **Retention:** exempt from purge (persist indefinitely).
- **Entry model:** one weight per day; re-logging the same day overwrites.
- **Unit:** pounds (lbs), stored and displayed. No unit preference.
- **Entry point:** a card on the Home screen (no new nav route, no Settings change).
- **Log UI:** lightweight `AlertDialog` with a single numeric field.

## Data layer

### Entity — `data/local/entity/WeightEntry.kt`

```kotlin
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey val date: LocalDate,   // one row per day; re-logging overwrites
    val weightLbs: Float,
    val recordedAt: Instant
)
```

`date` as the primary key enforces "one weight per day" at the schema level.
Insert uses `OnConflictStrategy.REPLACE` so re-logging overwrites the day's value.
`LocalDate`/`Instant` are already handled by the existing `Converters`.

### DAO — `data/local/dao/WeightDao.kt`

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsert(entry: WeightEntry)

@Query("SELECT * FROM weight_entries WHERE date = :date")
fun getForDate(date: LocalDate): Flow<WeightEntry?>

@Query("SELECT * FROM weight_entries ORDER BY date DESC")
fun getRecent(): Flow<List<WeightEntry>>   // for future stats/history

@Query("DELETE FROM weight_entries WHERE date = :date")
suspend fun delete(date: LocalDate)
```

### Repository — `data/repository/WeightRepository.kt`

`@Singleton` thin pass-through over `WeightDao`, mirroring `DailyLogRepository`
(`upsert`, `getForDate`, `getRecent`, `delete`).

### Database migration

- Bump `AppDatabase` to **version 4**.
- Add `WeightEntry::class` to the `@Database` entities array.
- Add `abstract fun weightDao(): WeightDao`.
- Add `MIGRATION_3_4`:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS weight_entries (" +
                "date TEXT NOT NULL PRIMARY KEY, " +
                "weightLbs REAL NOT NULL, " +
                "recordedAt INTEGER NOT NULL)"
        )
    }
}
```

- Register `MIGRATION_3_4` in `AppModule.provideDatabase` `.addMigrations(...)`.
- Add `provideWeightDao(db: AppDatabase): WeightDao = db.weightDao()` to `AppModule`.

Column SQL types match how the existing `Converters` serialize the fields
(confirmed against `Converters.kt`): `LocalDate` → TEXT (`toString`), `Instant`
→ INTEGER (epoch millis), `Float` → REAL.

### Purge exemption

`DailyResetWorker` only touches `log_entries` and requires no change. Add a code
comment noting weight is intentionally exempt from the purge so a future edit
doesn't "helpfully" add weight cleanup.

## UI

### `WeightCard` — `ui/home/components/WeightCard.kt`

Placed in the Home `LazyColumn` immediately after `MacroSummary` (before the
"Copy yesterday's meals" item).

- **Has today's weight:** shows e.g. "Weight: 178.4 lbs"; tapping the card opens
  the log dialog pre-filled for editing.
- **No weight today:** shows a "Log today's weight" prompt; tapping opens the
  empty log dialog.

### Log dialog

A Material 3 `AlertDialog` with a single numeric `TextField` (decimal keyboard),
pre-filled with today's value when one exists, plus Save / Cancel. Input is
validated to a positive `Float`; empty/invalid input disables Save.

### `HomeViewModel` wiring

- Inject `WeightRepository`.
- Add `todayWeight: WeightEntry? = null` to `HomeUiState`.
- Fold `weightRepository.getForDate(today)` into the existing `combine` (4 → 5
  flows; same arity-based overload).
- Add `fun logWeight(lbs: Float)` → `upsert(WeightEntry(date = today,
  weightLbs = lbs, recordedAt = Instant.now()))`.

No new navigation routes; no Settings changes.

## Testing

Follows existing patterns: JUnit 5 + MockK + Turbine + `androidx.room:room-testing`,
Robolectric for DAO tests, all on the JVM.

- **`WeightDaoTest`** (Robolectric + in-memory Room):
  - upsert then `getForDate` returns the entry.
  - re-upserting the same date overwrites (one row; `getRecent` has one row with
    the new value).
  - `getForDate` returns `null` when absent.
  - `getRecent` ordered by date descending.
  - `delete(date)` removes it.
- **`MIGRATION_3_4` test** via `MigrationTestHelper`: migration runs and the
  `weight_entries` table exists.
- **`HomeViewModelTest` additions:**
  - `logWeight(x)` calls `weightRepository.upsert` with the right date/value.
  - `todayWeight` surfaces into `HomeUiState` when the repo flow emits.
- **Purge safety:** confirm (via `DailyResetWorkerTest`) that the worker never
  references weight, guarding against regressions.

No Compose UI tests for the card — consistent with the current suite (no Compose
test infrastructure exists).

## Out of scope

- Statistics, charts, trends, averages.
- Unit preference (kg) / conversion.
- Multiple weigh-ins per day.
- Editing/deleting weight for arbitrary past dates via UI (only today is logged
  from the card; the DAO supports more for future work).
