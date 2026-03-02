# Calorie Counter — Project Overview

## Concept

A simple, focused Android calorie counter that tracks only today's intake. No history, no trends, no complexity — just a clear answer to "how am I doing today?"

## Data Source

- **OpenFoodFacts.org** — open-source food database for nutritional information
- Local cache of previously searched items for speed and offline use
- Manual entry fallback when items aren't found

---

## Core Features

### Daily Tracking

- Four meal categories: **Breakfast, Lunch, Dinner, Snacks**
- Configurable daily calorie goal
- Progress bar/ring showing calories consumed vs. remaining
- Running macro breakdown (protein, carbs, fat) alongside calorie totals
- Quick-add button for entering raw calorie values without searching for a specific food

### Food Search & Entry

- **Barcode scanner** for fast product lookup via OpenFoodFacts
- Text search with fuzzy matching and autocomplete
- Adjustable serving size and quantity on each item
- If a barcode isn't found, prompt for manual calorie entry (with optional contribution back to OpenFoodFacts)

### Standard Meals

- Save reusable meal presets per category (e.g., "Weekday Breakfast," "Weekend Breakfast")
- One-tap button within each meal category to load a saved meal's items for the day
- Multiple saved meals allowed per category
- Long-press a saved meal to edit it in place
- Recent items list for quickly re-adding frequently eaten one-off foods

---

## UX & Interaction

- **Swipe-to-remove** on individual items within a meal category
- **Tap to adjust** serving size or quantity after an item has been added
- **Daily reset** at midnight (or configurable reset time for night-shift schedules)
- **Copy yesterday** — temporarily cache the previous day's meals so similar days can be replicated with one tap; auto-purges after use
- **Home screen widget** showing today's calorie total at a glance

---

## Notifications

- Optional daily reminder if no meal has been logged by a configurable time

---

## Technical Notes

- **Platform:** Android (native Kotlin)
- **Local caching:** Store OpenFoodFacts search results in a local database for instant repeat lookups and offline capability
- **Data retention:** Only today's log and (temporarily) yesterday's log are stored; no long-term history
- **Architecture:** TBD — likely MVVM with Room for local persistence

---

## Out of Scope (Intentionally)

- Historical logs and trend graphs
- Social features or sharing
- Export functionality
- Meal planning or recipe management
- Integration with fitness trackers or health platforms
