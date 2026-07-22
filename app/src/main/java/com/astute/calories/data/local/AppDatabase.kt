package com.astute.calories.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astute.calories.data.local.dao.DailyLogDao
import com.astute.calories.data.local.dao.FoodCacheDao
import com.astute.calories.data.local.dao.SavedMealDao
import com.astute.calories.data.local.dao.WeightDao
import com.astute.calories.data.local.entity.CachedFood
import com.astute.calories.data.local.entity.LogEntry
import com.astute.calories.data.local.entity.SavedMeal
import com.astute.calories.data.local.entity.WeightEntry

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

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_foods ADD COLUMN servingSizeLabel TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_foods ADD COLUMN searchQuery TEXT DEFAULT NULL")
            }
        }

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
    }
}
