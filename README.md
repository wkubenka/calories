# Calories

A simple, focused Android calorie counter that tracks only today's intake. No history, no trends, no complexity — just a clear answer to "how am I doing today?"

## Features

- **Daily tracking** across four meal categories: Breakfast, Lunch, Dinner, Snacks
- **Food search** powered by [OpenFoodFacts](https://openfoodfacts.org) with local caching for offline use
- **Barcode scanner** for fast product lookup via ML Kit
- **Manual entry** for quick-adding calories when a food isn't in the database
- **Saved meals** — reusable meal templates you can load with one tap
- **Calorie progress ring** and macro breakdown (protein, carbs, fat)
- **Copy yesterday** — replicate the previous day's meals
- **Daily reset** at midnight (or a configurable time)
- **Reminder notifications** if you haven't logged by a set time
- Swipe-to-dismiss and tap-to-edit on logged items

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM + single-Activity with Compose Navigation
- Hilt for dependency injection
- Room for local persistence
- Retrofit + Moshi for OpenFoodFacts API
- CameraX + ML Kit for barcode scanning
- WorkManager for daily reset and reminders
- JUnit 5, MockK, Turbine for testing

## Building

```
./gradlew assembleDebug
```

Min SDK 26 (Android 8.0) · Target SDK 35 · Java 17

## Project Structure

```
app/src/main/java/com/astute/calories/
├── data/
│   ├── local/          # Room database, DAOs, entities, DataStore prefs
│   ├── remote/         # OpenFoodFacts API interface and DTOs
│   └── repository/     # FoodRepository, DailyLogRepository, SavedMealRepository
├── di/                 # Hilt modules
├── ui/
│   ├── home/           # Main daily view with progress ring and meal cards
│   ├── search/         # Food search with serving size picker
│   ├── scanner/        # Barcode scanner
│   ├── entry/          # Manual entry and serving size sheets
│   ├── meals/          # Saved meal management
│   ├── settings/       # Calorie goal, reset time, reminders
│   ├── navigation/     # Nav graph
│   └── theme/          # Colors, typography, Material 3 theme
├── worker/             # DailyResetWorker, ReminderWorker
└── util/
```

## Data Source

All nutritional data comes from [OpenFoodFacts](https://world.openfoodfacts.org), an open-source food database. Search results are cached locally in Room for faster repeat lookups and offline access.

## Design Decisions

- **No long-term history** — only today and yesterday are stored. The daily reset worker enforces this.
- **Cache-first search** — local database is checked before hitting the API.
- **Saved meals as JSON** — simpler than a junction table for a small dataset, serialized with Moshi.
