# Calorie Counter — Implementation Plan

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Build:** Gradle with Kotlin DSL, version catalogs
- **Architecture:** MVVM + single-Activity with Jetpack Compose navigation
- **DI:** Hilt
- **Local DB:** Room
- **Networking:** Retrofit + Moshi (OkHttp for caching)
- **Barcode scanning:** ML Kit barcode scanning (CameraX for preview)
- **Image loading:** Coil (for food product images from OpenFoodFacts)
- **Widget:** Glance (Jetpack Compose-based widget API)
- **Testing:** JUnit 5, Turbine (Flow testing), MockK

---

## Project Structure

```
app/src/main/java/com/calories/
├── CalorieApp.kt                  # Application class (Hilt entry point)
├── MainActivity.kt                # Single activity
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room database
│   │   ├── dao/
│   │   │   ├── FoodCacheDao.kt
│   │   │   ├── DailyLogDao.kt
│   │   │   └── SavedMealDao.kt
│   │   └── entity/
│   │       ├── CachedFood.kt
│   │       ├── LogEntry.kt
│   │       └── SavedMeal.kt
│   ├── remote/
│   │   ├── OpenFoodFactsApi.kt    # Retrofit interface
│   │   └── dto/                   # API response models
│   └── repository/
│       ├── FoodRepository.kt
│       ├── DailyLogRepository.kt
│       └── SavedMealRepository.kt
├── di/
│   ├── AppModule.kt               # Database, network singletons
│   └── RepositoryModule.kt
├── ui/
│   ├── navigation/
│   │   └── AppNavGraph.kt
│   ├── home/
│   │   ├── HomeScreen.kt          # Main daily view
│   │   ├── HomeViewModel.kt
│   │   └── components/
│   │       ├── CalorieProgressRing.kt
│   │       ├── MacroSummary.kt
│   │       └── MealCategoryCard.kt
│   ├── search/
│   │   ├── FoodSearchScreen.kt
│   │   ├── FoodSearchViewModel.kt
│   │   └── components/
│   │       └── FoodResultItem.kt
│   ├── scanner/
│   │   ├── BarcodeScannerScreen.kt
│   │   └── BarcodeScannerViewModel.kt
│   ├── entry/
│   │   ├── ManualEntrySheet.kt    # Bottom sheet for quick-add / manual
│   │   └── ServingSizeSheet.kt    # Adjust serving/quantity
│   ├── meals/
│   │   ├── SavedMealsScreen.kt
│   │   └── SavedMealsViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
├── widget/
│   └── CalorieWidget.kt           # Glance widget
├── worker/
│   └── DailyResetWorker.kt        # WorkManager for midnight reset
└── util/
    └── DateUtils.kt
```

---

## Phases

### Phase 1 — Project Skeleton & Local Data Layer

Set up the project foundation so everything after this can run on-device with no network.

1. Initialize Android project with Compose, Hilt, Room, and version catalog
2. Define Room entities: `LogEntry`, `CachedFood`, `SavedMeal`
3. Build DAOs and `AppDatabase`
4. Create repository classes with in-memory fakes for testing
5. Wire Hilt modules (`AppModule`, `RepositoryModule`)
6. Set up single-Activity with Compose navigation shell (empty screens)

**Deliverable:** App compiles and launches to a blank Home screen. Database migrations run. Unit tests pass for DAOs.

---

### Phase 2 — Home Screen & Daily Tracking

The core screen users see — today's calories, macros, and meal categories.

1. `HomeViewModel` — exposes today's log as `StateFlow`, grouped by meal category
2. `CalorieProgressRing` — animated ring showing consumed / goal
3. `MacroSummary` — row of protein / carbs / fat totals
4. `MealCategoryCard` — expandable card per category listing added items
5. Swipe-to-remove on individual items (SwipeToDismiss composable)
6. Tap an item to open `ServingSizeSheet` for editing quantity
7. Quick-add FAB → opens `ManualEntrySheet` (raw calorie + optional macro input)
8. Calorie goal stored in DataStore preferences

**Deliverable:** User can manually add calorie entries, see progress ring update, swipe to remove, and adjust items.

---

### Phase 3 — Food Search & OpenFoodFacts Integration

Connect to real food data.

1. Define Retrofit interface for OpenFoodFacts (`/cgi/search.pl`, `/api/v0/product/{barcode}.json`)
2. DTO models mapping API responses to domain `FoodItem`
3. `FoodRepository` — searches API, caches results in Room, returns cached hits first
4. `FoodSearchScreen` — search bar with debounced input, result list
5. Tap a result → `ServingSizeSheet` → confirm → adds `LogEntry`
6. Recent items query (last N distinct foods added) shown above search results
7. OkHttp cache layer for offline fallback on network responses

**Deliverable:** User can search foods by name, see nutritional info, adjust serving, and add to today's log.

---

### Phase 4 — Barcode Scanner

Fast product lookup via camera.

1. Add CameraX + ML Kit barcode dependencies
2. `BarcodeScannerScreen` — camera preview with overlay
3. On barcode detected → query OpenFoodFacts by barcode
4. If found → show product details with serving size picker → add to log
5. If not found → prompt for manual calorie entry (offer optional OpenFoodFacts contribution link)

**Deliverable:** User can scan a barcode and add the product or fall back to manual entry.

---

### Phase 5 — Saved Meals

Reusable meal presets for speed.

1. `SavedMeal` entity — name, category, list of food items (as JSON blob or junction table)
2. `SavedMealsScreen` — list of saved meals grouped by category
3. "Save current meal" action on each `MealCategoryCard`
4. One-tap "Load saved meal" button within each category on Home screen
5. Long-press a saved meal → inline edit (rename, add/remove items)
6. Multiple saved meals per category

**Deliverable:** User can save, load, edit, and delete meal presets.

---

### Phase 6 — Daily Reset, Copy Yesterday, and Notifications

Time-based behaviors.

1. `DailyResetWorker` (WorkManager) — runs at midnight (or configurable time)
   - Copies today's log to a "yesterday" cache
   - Clears today's log
   - Purges any data older than yesterday
2. "Copy yesterday" button on Home screen — loads yesterday's cached entries into today
3. Settings screen: configurable reset time, calorie goal, reminder time
4. Notification via WorkManager — fires if no log entry exists by reminder time

**Deliverable:** App resets daily, yesterday's meals can be replicated, and reminders fire on schedule.

---

### Phase 7 — Home Screen Widget

At-a-glance calorie total.

1. `CalorieWidget` using Jetpack Glance
2. Displays: calories consumed / goal, small progress bar
3. Updates on each log change via `AppWidgetManager` callback from repository
4. Tap widget → opens app to Home screen

**Deliverable:** Home screen widget shows live calorie total.

---

### Phase 8 — Polish & Edge Cases

1. Dark/light theme with Material 3 dynamic color
2. Keyboard and accessibility pass (content descriptions, focus order)
3. Error states: no network, empty search results, camera permission denied
4. Animations: ring fill, item add/remove transitions
5. ProGuard/R8 rules for Retrofit, Moshi, Room
6. Manual testing pass on a range of screen sizes

**Deliverable:** Production-quality app ready for personal use or Play Store.

---

## Data Models (Room Entities)

```kotlin
@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,              // today or yesterday only
    val mealCategory: MealCategory,   // BREAKFAST, LUNCH, DINNER, SNACKS
    val foodName: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSize: Float,           // in grams or ml
    val quantity: Float,              // multiplier
    val barcode: String? = null,
    val addedAt: Instant
)

@Entity(tableName = "cached_foods")
data class CachedFood(
    @PrimaryKey val barcode: String,
    val name: String,
    val calories: Int,                // per 100g
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSizeG: Float?,
    val imageUrl: String?,
    val lastAccessed: Instant
)

@Entity(tableName = "saved_meals")
data class SavedMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: MealCategory,
    val itemsJson: String             // JSON array of food items
)
```

---

## OpenFoodFacts API Endpoints

| Purpose | Method | URL |
|---|---|---|
| Search by name | GET | `https://world.openfoodfacts.org/cgi/search.pl?search_terms={query}&json=1&page_size=20` |
| Lookup by barcode | GET | `https://world.openfoodfacts.org/api/v0/product/{barcode}.json` |

Key response fields: `product.product_name`, `product.nutriments.energy-kcal_100g`, `product.nutriments.proteins_100g`, `product.nutriments.carbohydrates_100g`, `product.nutriments.fat_100g`, `product.serving_size`, `product.image_url`.

---

## Key Design Decisions

- **No long-term history** — Room DB only holds today + yesterday. `DailyResetWorker` enforces this.
- **Offline-first** — Food cache in Room means repeat lookups work without network. OkHttp cache covers recent API responses.
- **Saved meals stored as JSON** — Simpler than a junction table for a small data set. Parsed with Moshi.
- **Single Activity + Compose Nav** — Standard modern Android pattern. Bottom sheets for quick entry keep the user in context.
- **WorkManager for reset/reminders** — Survives process death and respects Doze mode.
