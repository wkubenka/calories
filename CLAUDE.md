# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android calorie counter — single-Activity Jetpack Compose app (`com.astute.calories`, "Astute Calories"). Min SDK 26, target SDK 35, Java/Kotlin 17. The defining product constraint: **only today's and yesterday's data are retained**. Anything older is purged by `DailyResetWorker`. Don't add features that assume long-term history.

## Common commands

```bash
./gradlew assembleDebug                  # debug APK
./gradlew assembleRelease                # release APK (needs keystore env vars + FDA key)
./gradlew test                           # all unit tests (JUnit 5 / Jupiter)
./gradlew :app:testDebugUnitTest         # debug variant tests only
./gradlew :app:testDebugUnitTest --tests "com.astute.calories.ui.home.HomeViewModelTest"
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest.method name"
./gradlew lint                           # Android lint
```

Tests run on JVM (`useJUnitPlatform()`); there is no `androidTest/` instrumentation suite. `unitTests.isReturnDefaultValues = true`, so Android framework calls return defaults rather than throwing.

## Required local config

`local.properties` must define `FDA_API_KEY=...` (USDA FoodData Central). It's read in `app/build.gradle.kts` and exposed as `BuildConfig.FDA_API_KEY` via the `@FdaApiKey` Hilt qualifier. Without it, food search returns nothing useful — barcode lookup still works because it uses a different API.

Release signing is driven by env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Releases are tag-driven (`v*`) via `.github/workflows/release.yml`, which derives `versionCode` from the semver tag (`v1.2.3` → `10203`).

## Architecture

MVVM with Hilt DI, Compose Navigation, Room, Retrofit/Moshi, WorkManager. Code-gen is KSP (Hilt + Room). Three things are non-obvious from the file tree:

**1. Two food APIs, used for different purposes.** `FoodRepository` searches via the **FDA / USDA FoodData Central** API (`api.nal.usda.gov/fdc/v1/`) and looks up barcodes via **OpenFoodFacts** (`us.openfoodfacts.org/`) — the FDA API has no barcode endpoint. The README still says "powered by OpenFoodFacts" for search; that's outdated. Each API has its own `@Qualifier`-tagged Retrofit instance in `NetworkModule`. Search is **cache-first**: `FoodCacheDao.searchByName` is checked before any network call, and successful FDA responses are upserted into `cached_foods`. FDA descriptions arrive ALL CAPS and per-100g — `FoodRepository.toCachedFood` title-cases them and treats nutrient values as already-normalized.

**2. Room schema is versioned and migrating.** `AppDatabase` is at version 3 with explicit `MIGRATION_1_2` and `MIGRATION_2_3` (both add nullable columns to `cached_foods`). Any entity change needs a new migration — don't bump the version without one. Three entities: `LogEntry`, `CachedFood`, `SavedMeal`.

**3. SavedMeal stores its items as a JSON blob, not a junction table.** `SavedMealRepository` uses a Moshi adapter for `List<SavedMealItem>` and the column is plain text. This is intentional (see README "Design Decisions") — don't refactor it into a relational structure unless the dataset stops being small.

**Daily reset and reminders.** `CalorieApp.onCreate` calls `WorkScheduler.scheduleDailyReset(this, 0)` on every launch. `DailyResetWorker` deletes entries before *yesterday* (so yesterday is preserved for "Copy yesterday"). `ReminderWorker` fires the optional unlogged-meal reminder. App is `Configuration.Provider` and uses `HiltWorkerFactory` — workers must be `@HiltWorker` with `@AssistedInject`.

**Layering.** UI (`ui/<feature>/{Screen,ViewModel}.kt`) → repositories (`data/repository/`) → DAOs / Retrofit interfaces. ViewModels expose `Flow`/`StateFlow`; `DailyLogRepository` and `SavedMealRepository` return `Flow` directly from DAOs. `UserPreferences` (DataStore) holds calorie goal, reset time, reminder settings. Navigation is centralized in `ui/navigation/AppNavGraph.kt`.

## Testing notes

JUnit 5 + MockK + Turbine + `androidx.room:room-testing`. Existing tests cover the three repositories and `HomeViewModel` / `FoodSearchViewModel`. When adding a repository or ViewModel, follow the existing patterns rather than introducing a new mocking/assertion library.
